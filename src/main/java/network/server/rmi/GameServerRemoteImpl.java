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
 * RMI server endpoint ({@link GameServerRemote} implementation). Bridges RMI
 * clients to the single {@link LobbyManager}: it registers players via the "ask"
 * pattern, routes inbound {@link shared.command.ClientCommand}s, and dispatches
 * heartbeats to the matching {@link RmiVirtualView}'s liveness watchdog.
 */
public class GameServerRemoteImpl extends UnicastRemoteObject implements GameServerRemote {

    /** Maximum time to wait for the lobby-thread's registration verdict. */
    private static final long REGISTRATION_TIMEOUT_MS = 50_000L;

    /** The shared lobby/registry every RMI client is funneled into. */
    private final LobbyManager lobbyManager;
    /** playerName → in-game command queue, populated once the player joins a session. */
    private final ConcurrentHashMap<String, BlockingQueue<GameCommand>> gameQueues = new ConcurrentHashMap<>();
    /** playerName → {@link RmiVirtualView}, so a {@code HeartbeatCommand} refreshes the right liveness watchdog. */
    private final ConcurrentHashMap<String, RmiVirtualView> rmiViews =
            new ConcurrentHashMap<>();


    /** Routes each inbound {@code ClientCommand} by type (lobby / in-game / heartbeat). */
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
            // Isolated liveness channel: only HeartbeatCommand refreshes the server-side watchdog.
            RmiVirtualView view = rmiViews.get(cmd.playerName());
            if (view != null) view.notifyInbound();
        }
    };

    /**
     * @param lobbyManager the shared registry to funnel all RMI clients into
     * @throws RemoteException if exporting this remote object fails
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
            // "ask" pattern: enqueue the registration and block on the lobby-thread's verdict.
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
            // Name rejected (or registration failed): release the sender executor
            // the view just created, so it is not left dangling.
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
