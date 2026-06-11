package network.server.rmi;

import shared.command.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import network.client.rmi.ClientCallbackRemote;
import network.server.core.LobbyManager;
import shared.command.gameCommand.GameCommand;
import shared.command.lobbyCommand.HeartbeatCommand;
import shared.command.lobbyCommand.LobbyCommand;

/**
 * Server-side implementation of {@link GameServerRemote}. Exported as a
 * {@link UnicastRemoteObject} and registered in the RMI registry by
 * {@link network.server.core.ServerMain}.
 *
 * Incoming commands arrive on RMI worker threads and are dispatched
 * synchronously via the internal {@link ClientCommandVisitor}: lobby commands
 * go to {@link LobbyManager}, game commands are placed on the per-player
 * {@link #gameQueues} entry, and heartbeat commands notify the corresponding
 * {@link RmiVirtualView}'s liveness sentinel via {@link #rmiViews}.
 *
 * @see GameServerRemote
 * @see RmiPlayerEntry
 * @see RmiVirtualView
 */
public class GameServerRemoteImpl extends UnicastRemoteObject implements GameServerRemote {

    /** Maximum time to wait for the lobby thread to accept or reject a registration request. */
    private static final long REGISTRATION_TIMEOUT_MS = 50_000L;

    private final LobbyManager lobbyManager;

    /** Maps player name to the active game command queue for in-game command routing. */
    private final ConcurrentHashMap<String, BlockingQueue<GameCommand>> gameQueues = new ConcurrentHashMap<>();

    /**
     * Maps player name to {@link RmiVirtualView} for heartbeat routing.
     * {@link RmiVirtualView#notifyInbound()} is called directly here rather
     * than via the {@link network.server.core.VirtualView} interface, which
     * does not expose that method by design.
     */
    private final ConcurrentHashMap<String, RmiVirtualView> rmiViews =
            new ConcurrentHashMap<>();

    /**
     * Routes each incoming command to its destination: lobby commands go to
     * {@link LobbyManager}, game commands are placed on the player's game queue,
     * and heartbeat commands notify the corresponding {@link RmiVirtualView}'s
     * liveness sentinel.
     */
    private final ClientCommandVisitor dispatcher = new ClientCommandVisitor() {
        @Override
        public void visit(LobbyCommand cmd) {
            lobbyManager.submit(cmd);
        }

        @Override
        public void visit(GameCommand cmd) throws InterruptedException {
            BlockingQueue<GameCommand> queue = gameQueues.get(cmd.getPlayerName());
            if (queue == null) return;
            queue.put(cmd);
        }

        @Override
        public void visit(HeartbeatCommand cmd) {
            // Isolated liveness channel: only HeartbeatCommand updates the server-side watchdog.
            RmiVirtualView view = rmiViews.get(cmd.playerName());
            if (view != null) view.notifyInbound();
        }
    };

    /**
     * @param lobbyManager the lobby manager to forward commands and connection events to
     * @throws RemoteException if the RMI export fails
     */
    public GameServerRemoteImpl(LobbyManager lobbyManager) throws RemoteException {
        super();
        this.lobbyManager = lobbyManager;
    }

    /** Register the game queue for an RMI player. Called by {@link RmiPlayerEntry#setGameQueue}. */
    public void registerQueue(String playerName, BlockingQueue<GameCommand> queue) {
        gameQueues.put(playerName, queue);
    }

    @Override
    public boolean join(String playerName, ClientCallbackRemote callback, String host) throws RemoteException {
        RmiVirtualView view = new RmiVirtualView(playerName, callback, lobbyManager);
        boolean connectionSuccessful;
        try {
            // "Ask" pattern: enqueue the registration and block until the lobby thread returns a verdict.
            connectionSuccessful = lobbyManager.submitRmiRegistration(new RmiPlayerEntry(view, this))
                    .get(REGISTRATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | ExecutionException e) {
            connectionSuccessful = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            connectionSuccessful = false;
        }

        if (connectionSuccessful) {
            // Register the view so the HeartbeatCommand dispatcher can route
            // liveness notifications back to the right sentinel.
            rmiViews.put(playerName, view);
            System.out.println("RMI connection from " + host);
        } else {
            // Name rejected or registration failed: release the sender executor
            // created by the view so it does not leak.
            view.close();
        }
        return connectionSuccessful;
    }

    /**
     * Accepts a command from the RMI client and dispatches it via the
     * internal {@link #dispatcher}. Any routing exception is wrapped in a
     * {@link RemoteException} so the client receives a meaningful error.
     *
     * @param command the command to route
     * @throws RemoteException if command routing fails
     */
    @Override
    public void submitClientCommand(ClientCommand command) throws RemoteException {
        try {
            command.accept(dispatcher);
        } catch (Exception e) {
            throw new RemoteException("Command routing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Forwards a graceful disconnect request to the lobby manager's
     * disconnect pipeline.
     *
     * @param playerName the name of the disconnecting player
     * @throws RemoteException if the RMI call fails
     */
    @Override
    public void disconnect(String playerName) throws RemoteException {
        lobbyManager.onDisconnect(playerName);
    }
}
