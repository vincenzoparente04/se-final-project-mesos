package network.client.rmi;

import shared.command.gameCommand.ChooseColorCommand;
import shared.command.gameCommand.DrawCardCommand;
import shared.command.gameCommand.EndTurnCommand;
import shared.command.gameCommand.PlaceTotemCommand;
import shared.command.lobbyCommand.ListLobbiesCommand;
import shared.command.lobbyCommand.CreateLobbyCommand;
import shared.command.lobbyCommand.JoinLobbyCommand;
import shared.command.lobbyCommand.HeartbeatCommand;
import shared.command.lobbyCommand.LeaveCommand;
import shared.command.ClientCommand;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import network.client.core.LocalGameState;
import network.client.core.VirtualServer;
import network.client.core.ClientStateListener;
import network.server.rmi.GameServerRemote;
import shared.liveness.LivenessSentinel;


/**
 * RMI implementation of the client-side {@link VirtualServer} proxy.
 * <p>
 * Lifecycle:
 * <ol>
 *   <li>{@link #RmiVirtualServer(String, int)} looks up the server stub in the
 *       RMI registry. No callback is exported and no {@code join} call is
 *       made at this stage.</li>
 *   <li>{@link #tryRegisterName(String, LocalGameState, ClientStateListener)}
 *       exports a fresh {@link ClientCallbackImpl} bound to the given
 *       local-state/listener pair and calls {@code serverStub.join(...)}. If
 *       the server rejects the name (already taken), the callback is
 *       unexported and the method returns {@code false} so the caller can
 *       retry with a different name.</li>
 *   <li>{@link #start()} avvia il sentinel di liveness bidirezionale.</li>
 * </ol>
 *
 * @implNote The exported callback object owns an RMI listener thread that
 * keeps the JVM alive. {@link #close()} explicitly unexports it to allow clean process termination.
 */
public class RmiVirtualServer implements VirtualServer {

    private static final String SERVICE_NAME = "MesosGameServer";

    /**
     * Intervalli del sentinel client-side RMI.
     * Invariante: {@code TIMEOUT_MS > 2 * SEND_INTERVAL_MS} per tollerare il jitter di scheduling
     * e ritardi temporanei dovuti a messaggi applicativi grandi che impegnano il sender.
     * Il detection time massimo è {@code TIMEOUT_MS + CHECK_INTERVAL_MS = 12s}.
     */
    private static final long SEND_INTERVAL_MS  = 2_000L;
    private static final long CHECK_INTERVAL_MS = 5_000L;
    private static final long TIMEOUT_MS        = 15_000L;

    private final String host;
    private final GameServerRemote serverStub;

    private String playerName;
    private ClientCallbackImpl callback;
    private ClientStateListener listener;
    private ExecutorService commandExecutor;
    private LivenessSentinel sentinel;

    /**
     * Riferimento al notifier di liveness passato al {@link ClientCallbackImpl}.
     * Inizialmente è un no-op; viene aggiornato in {@link #start()} quando il sentinel
     * è pronto, evitando problemi di ordine di inizializzazione.
     */
    private final AtomicReference<Runnable> inboundNotifier = new AtomicReference<>(() -> {});

    /** Garantisce che la pipeline di chiusura venga eseguita al più una volta. */
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    public RmiVirtualServer(String host, int rmiPort) throws Exception {
        this.host = host;
        Registry registry = LocateRegistry.getRegistry(host, rmiPort);
        this.serverStub = (GameServerRemote) registry.lookup(SERVICE_NAME);
    }

    @Override
    public boolean tryRegisterName(String name, LocalGameState localState, ClientStateListener listener) {
        ClientCallbackImpl tempCallback;
        try {
            // Il notifier punta all'AtomicReference: quando start() imposta il sentinel,
            // tutte le callback successive trovano automaticamente il notifier aggiornato.
            tempCallback = new ClientCallbackImpl(localState, listener,
                    () -> inboundNotifier.get().run());
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to export RMI callback: " + e.getMessage(), e);
        }

        boolean accepted = false;
        try {
            accepted = serverStub.join(name, tempCallback, host);
        } catch (RemoteException e) {
            try { UnicastRemoteObject.unexportObject(tempCallback, true); } catch (RemoteException ignored) {}
            throw new RuntimeException("RMI join failed: " + e.getMessage(), e);
        }

        if (!accepted) {
            try {
                tempCallback.onError("Name already taken: " + name);
                UnicastRemoteObject.unexportObject(tempCallback, true);
            } catch (RemoteException ignored) {}
            return false;
        }

        this.listener = listener;
        this.playerName = name;
        this.callback = tempCallback;
        return true;
    }

    @Override
    public void start() {
        this.commandExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rmi-commands-" + playerName);
            t.setDaemon(true);
            return t;
        });

        this.sentinel = new LivenessSentinel(
                "client-" + playerName,
                SEND_INTERVAL_MS, CHECK_INTERVAL_MS, TIMEOUT_MS,
                () -> submitAsync(new HeartbeatCommand(playerName)),
                this::onConnectionLost);
        sentinel.start();
        //
        inboundNotifier.set(sentinel::notifyInbound);
    }

    // Game commands
    @Override
    public void sendChooseColor(String color) {
        submitAsync(new ChooseColorCommand(playerName, color));
    }
    @Override
    public void sendPlaceTotem(char tile) {
        submitAsync(new PlaceTotemCommand(playerName, tile));
    }
    @Override
    public void sendDrawCard(int cardId) {
        submitAsync(new DrawCardCommand(playerName, cardId));
    }
    @Override
    public void sendEndTurn() {
        submitAsync(new EndTurnCommand(playerName));
    }

    // Lobby commands
    @Override
    public void sendCreateLobby(int playersNumber) {
        submitAsync(new CreateLobbyCommand(playerName, playersNumber));
    }
    @Override
    public void sendJoinLobby(String lobbyId) {
        submitAsync(new JoinLobbyCommand(playerName, lobbyId));
    }
    @Override
    public void sendListLobbies() {
        submitAsync(new ListLobbiesCommand(playerName));
    }

    @Override
    public void sendLeaveCommand() {
        submitAsync(new LeaveCommand(playerName));
    }

    @Override
    public void close() {
        if (!disconnected.compareAndSet(false, true)) return;
        if (sentinel != null) sentinel.stop();
        // Notifica il server della disconnessione
        try {
            if (playerName != null) serverStub.disconnect(playerName);
        } catch (RemoteException e) {
            System.err.println("Disconnect notification failed: " + e.getMessage());
        }
        cleanupLocalResources();
        if (listener != null) listener.onDisconnected();
        System.exit(0);
    }

    /**
     * Invocata dal watchdog quando il server non invia messaggi entro {@code TIMEOUT_MS}.
     * Ferma il sentinel, libera le risorse locali e notifica l'interfaccia utente.
     */
    private void onConnectionLost() {
        if (!disconnected.compareAndSet(false, true)) return;
        if (sentinel != null) sentinel.stop();
        cleanupLocalResources();
        if (listener != null) listener.onDisconnected();
        System.exit(0);
    }

    /**
     * Tears down the local resources (executor + exported callback) without
     * notifying the server. Used both by {@link #close()} after a successful
     * session and by the constructor's failure path, where the server has no
     * record of this client and a {@code disconnect} call would be incorrect.
     */
    private void cleanupLocalResources() {
        if (commandExecutor != null) {
            commandExecutor.shutdown();
            try {
                if (!commandExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    commandExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                commandExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (callback != null) {
            try {
                UnicastRemoteObject.unexportObject(callback, true);
            } catch (RemoteException ignored) {}
        }
    }

    /**
     * Serialises the given command onto the command executor. If the
     * executor has already been shut down (because {@link #close()} was
     * called), the command is silently dropped.
     */
    private void submitAsync(ClientCommand command) {
        if (commandExecutor == null) return;
        commandExecutor.submit(() -> {
            try {
                serverStub.submitClientCommand(command);
            } catch (RemoteException e) {
                System.err.println("RMI command failed: " + e.getMessage());
                onConnectionLost();
            }
        });
    }
}
