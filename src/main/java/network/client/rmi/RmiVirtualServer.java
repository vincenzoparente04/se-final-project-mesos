package network.client.rmi;

import shared.command.ChooseColorCommand;
import shared.command.ClientCommand;
import shared.command.CreateLobbyCommand;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.JoinLobbyCommand;
import shared.command.LeaveCommand;
import shared.command.ListLobbiesCommand;
import shared.command.PlaceTotemCommand;
import shared.command.HeartbeatCommand;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;

import network.client.LocalGameState;
import network.client.VirtualServer;
import network.client.ClientStateListener;
import network.server.rmi.GameServerRemote;


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
 *   <li>{@link #start()} starts the heartbeat scheduler.</li>
 * </ol>
 *
 * @implNote The exported callback object owns an RMI listener thread that
 * keeps the JVM alive. {@link #close()} explicitly unexports it to allow clean process termination.
 */
public class RmiVirtualServer implements VirtualServer {

    private static final String SERVICE_NAME = "MesosGameServer";

    /** Periodo di invio heartbeat: deve essere < del timeout server (6s). */
    private static final long HEARTBEAT_INTERVAL_MS = 2_000L;

    private final String host;
    private final GameServerRemote serverStub;

    private String playerName;
    private ClientCallbackImpl callback;
    private ExecutorService commandExecutor;
    private ScheduledExecutorService heartbeatScheduler;

    public RmiVirtualServer(String host, int rmiPort) throws Exception {
        this.host = host;
        Registry registry = LocateRegistry.getRegistry(host, rmiPort);
        this.serverStub = (GameServerRemote) registry.lookup(SERVICE_NAME);
    }

    @Override
    public boolean tryRegisterName(String name, LocalGameState localState, ClientStateListener listener) {
        ClientCallbackImpl tempCallback;
        try {
            tempCallback = new ClientCallbackImpl(localState, listener);
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to export RMI callback: " + e.getMessage(), e);
        }

        boolean accepted;
        try {
            accepted = serverStub.join(name, tempCallback, host);
        } catch (RemoteException e) {
            try { UnicastRemoteObject.unexportObject(tempCallback, true); } catch (RemoteException ignored) {}
            throw new RuntimeException("RMI join failed: " + e.getMessage(), e);
        }

        if (!accepted) {
            try { UnicastRemoteObject.unexportObject(tempCallback, true); } catch (RemoteException ignored) {}
            return false;
        }

        this.playerName = name;
        this.callback = tempCallback; // TODO a che serve callback in questa classe?
        return true;
    }

    @Override
    public void start() {
        this.commandExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rmi-commands-" + playerName);
            t.setDaemon(true);
            return t;
        });

        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-sender-" + playerName);
            t.setDaemon(true);
            return t;
        });

        this.heartbeatScheduler.scheduleAtFixedRate(
                () -> submitAsync(new HeartbeatCommand(playerName)),
                HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
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
    public void sendCreateLobby(int maxPlayers) {
        submitAsync(new CreateLobbyCommand(playerName, maxPlayers));
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
        if (heartbeatScheduler != null) heartbeatScheduler.shutdownNow();
        // 1. Notifica il server della disconnessione
        try {
            if (playerName != null) serverStub.disconnect(playerName);
        } catch (RemoteException e) {
            System.err.println("Disconnect notification failed: " + e.getMessage());
        }
        cleanupLocalResources();
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
            }
        });
    }
}
