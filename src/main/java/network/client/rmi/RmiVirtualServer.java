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

import java.rmi.NotBoundException;
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
 *   <li>{@link #start()} starts the bidirectional liveness sentinel.</li>
 * </ol>
 * <p>
 * The exported callback object owns an RMI listener thread that keeps the JVM
 * alive. {@link #close()} explicitly unexports it to allow clean process termination.
 */
public class RmiVirtualServer implements VirtualServer {

    private static final String SERVICE_NAME = "MesosGameServer";

    /**
     * Client-side RMI liveness intervals (milliseconds). Invariant:
     * {@code TIMEOUT_MS > 2 * SEND_INTERVAL_MS}, to tolerate scheduling jitter and
     * brief delays from large application messages occupying the sender. Worst-case
     * detection time is {@code TIMEOUT_MS + CHECK_INTERVAL_MS = 20 s}.
     */
    private static final long SEND_INTERVAL_MS  = 2_000L;
    private static final long CHECK_INTERVAL_MS = 5_000L;
    private static final long TIMEOUT_MS        = 15_000L;

    /** Server host, re-used as the advertised callback host in {@link #tryRegisterName}. */
    private final String host;
    /** Remote stub looked up in the registry; the only server entry point. */
    private final GameServerRemote serverStub;

    /** Registered player name; set on a successful {@link #tryRegisterName}. */
    private String playerName;
    /** This client's exported callback; the server pushes messages through it. */
    private ClientCallbackImpl callback;
    /** Listener notified of inbound server events. */
    private ClientStateListener listener;
    /** Single-thread executor that serialises outbound commands; created in {@link #start()}. */
    private ExecutorService commandExecutor;
    /** Bidirectional liveness watchdog; created in {@link #start()}. */
    private LivenessSentinel sentinel;

    /**
     * Indirection for the liveness notifier passed to {@link ClientCallbackImpl}.
     * Initially a no-op; {@link #start()} swaps in the real sentinel once it is
     * ready, avoiding an initialization-order problem (callbacks may arrive before
     * {@code start()} completes).
     */
    private final AtomicReference<Runnable> inboundNotifier = new AtomicReference<>(() -> {});

    /** Ensures the shutdown pipeline runs at most once. */
    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    /**
     * Looks up the server stub in the RMI registry. No callback is exported and no
     * {@code join} call is made yet.
     *
     * @param host the server hostname or IP
     * @param rmiPort the RMI registry port
     * @throws Exception if the registry is unreachable or the service is not bound
     */
    public RmiVirtualServer(String host, int rmiPort) throws Exception {
        this.host = host;
        try {
            Registry registry = LocateRegistry.getRegistry(host, rmiPort);
            this.serverStub = (GameServerRemote) registry.lookup(SERVICE_NAME);
        } catch (NotBoundException e) {
            throw new Exception("RMI service '" + SERVICE_NAME + "' not bound on " + host + ":" + rmiPort, e);
        } catch (RemoteException e) {
            throw new Exception("Cannot reach RMI registry at " + host + ":" + rmiPort + " (" + e.getMessage() + ")", e);
        }
    }

    /**
     * RMI name handshake: exports a fresh {@link ClientCallbackImpl} and calls
     * {@code serverStub.join(...)}. On rejection the callback is unexported at once,
     * so a retry exports a clean one. No reader thread is involved — the RMI runtime
     * drives inbound callbacks.
     */
    @Override
    public boolean tryRegisterName(String name, LocalGameState localState, ClientStateListener listener) {
        ClientCallbackImpl tempCallback;
        try {
            // The notifier reads the AtomicReference: once start() installs the sentinel,
            // every later callback automatically finds the updated notifier.
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

    /**
     * Creates the single-thread command executor, starts the liveness sentinel and
     * routes inbound notifications to it. No reader thread is spawned (the RMI
     * runtime delivers callbacks). Call once, after a successful {@link #tryRegisterName}.
     */
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
        // Now that the sentinel exists, route inbound notifications to it.
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

    /**
     * Closes the connection once (idempotent): stops the sentinel, notifies the
     * server via {@code serverStub.disconnect}, unexports the callback and the
     * command executor, notifies the listener and terminates the client process.
     */
    @Override
    public void close() {
        if (!disconnected.compareAndSet(false, true)) return;
        if (sentinel != null) sentinel.stop();
        // Notify the server of the disconnect.
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
     * Invoked by the watchdog when the server sends no message within
     * {@code TIMEOUT_MS}. Stops the sentinel, releases local resources and notifies
     * the UI. Runs at most once (guarded by {@link #disconnected}).
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
