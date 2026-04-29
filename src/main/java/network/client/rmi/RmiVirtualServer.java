package network.client.rmi;

import shared.command.ChooseColorCommand;
import shared.command.ClientCommand;
import shared.command.CreateLobbyCommand;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.JoinLobbyCommand;
import shared.command.ListLobbiesCommand;
import shared.command.PlaceTotemCommand;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import network.client.LocalGameState;
import network.client.VirtualServer;
import network.client.ClientStateListener;
import network.server.rmi.GameServerRemote;


/**
 * RMI implementation of the client-side {@link VirtualServer} proxy.
 * <p>
 * Each instance represents a single client's session of a remote game server.
 * Construction performs the full handshake: it looks up the server
 * stub in the RMI registry, exports a {@link ClientCallbackImpl} so the
 * server can push state updates back, and registers the player by calling
 * {@link GameServerRemote#join(String, ClientCallbackRemote)}.
 * <p>
 * <b>Threading model.</b> All outbound commands are dispatched through a
 * dedicated single-thread executor. This serves two purposes: it prevents
 * the calling thread (typically the UI thread) from blocking on the synchronous
 * RMI call, and it preserves the order in which commands were issued.
 * Inbound callbacks from the server are delivered on RMI worker threads
 * inside {@link ClientCallbackImpl}; this class does not own those threads.
 *
 * @implNote The exported callback object owns an RMI listener thread that
 * keeps the JVM alive. {@link #close()} explicitly unexports it to allow clean process termination.
 */
public class RmiVirtualServer implements VirtualServer {

    private static final String SERVICE_NAME = "MesosGameServer";

    private final String playerName;
    private final GameServerRemote serverStub;
    private final ClientCallbackImpl callback;
    private final ExecutorService commandExecutor;


    /**
     * Connects to the remote server, exports the local callback, and join the server with the given player name.
     * @throws Exception if the registry lookup fails, the callback cannot
     * be exported, or the {@code join} call is rejected by the server (e.g. duplicate name)
     */
    public RmiVirtualServer(String host, int rmiPort, String playerName, LocalGameState localState, ClientStateListener listener) throws Exception {
        this.playerName = playerName;

        Registry registry = LocateRegistry.getRegistry(host, rmiPort);
        // controlla se questo cast è inevitabile
        this.serverStub = (GameServerRemote) registry.lookup(SERVICE_NAME);

        this.callback = new ClientCallbackImpl(localState, listener);

        this.commandExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rmi-commands-" + playerName);
            t.setDaemon(true);
            return t;
        });

        try {
            serverStub.join(playerName, callback);
        } catch (RemoteException e) {
            cleanupLocalResources();
            throw e;
        }
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
    public void close() {
        try {
            serverStub.disconnect(playerName);
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
        commandExecutor.shutdown();
        try {
            if (!commandExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                commandExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            commandExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            UnicastRemoteObject.unexportObject(callback, true);
        } catch (RemoteException ignored) {}
    }

    /**
     * Serialises the given command onto the command executor. If the
     * executor has already been shut down (because {@link #close()} was
     * called), the command is silently dropped.
     */
    private void submitAsync(ClientCommand command) {
        commandExecutor.submit(() -> {
            try {
                serverStub.submitClientCommand(command);
            } catch (RemoteException e) {
                System.err.println("RMI command failed: " + e.getMessage());
            }
        });
    }
}
