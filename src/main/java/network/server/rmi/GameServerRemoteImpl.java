package server.rmi;

import server.core.LobbyManager;
import shared.command.GameCommand;
import shared.rmi.ClientCallbackRemote;
import shared.rmi.GameServerRemote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RMI server implementation of {@link GameServerRemote}.
 * <p>
 * Registered in the RMI registry by {@link server.core.ServerMain}.
 * <ul>
 *   <li>{@link #join} creates an {@link RmiVirtualView} (backed by the
 *       caller's callback stub), wraps it in an {@link RmiPlayerEntry}, and
 *       forwards it to the {@link LobbyManager}.</li>
 *   <li>{@link #submitCommand} places the command on the shared
 *       {@link BlockingQueue} and returns immediately, keeping the client
 *       unblocked.  The {@link server.core.GameThread} processes commands
 *       sequentially from the other end of the queue.</li>
 * </ul>
 * One instance serves all connections; the shared queue and lobby are
 * injected via the constructor.
 */
public class GameServerRemoteImpl extends UnicastRemoteObject implements GameServerRemote {

    private final LobbyManager lobbyManager;
    private final BlockingQueue<GameCommand> commandQueue;
    private final ExecutorService callbackExecutor;

    public GameServerRemoteImpl(LobbyManager lobbyManager,
                                BlockingQueue<GameCommand> commandQueue)
            throws RemoteException {
        super();
        this.lobbyManager = lobbyManager;
        this.commandQueue = commandQueue;
        this.callbackExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "rmi-callback");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void join(String playerName, ClientCallbackRemote callback)
            throws RemoteException {
        RmiVirtualView view = new RmiVirtualView(playerName, callback, callbackExecutor);
        lobbyManager.addRmiPlayer(new RmiPlayerEntry(view));
    }

    /**
     * Enqueues the command and returns immediately.
     * The client is unblocked within 1-2 ms regardless of how long the
     * server takes to process the command.
     */
    @Override
    public void submitCommand(GameCommand command) throws RemoteException {
        try {
            commandQueue.put(command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Server interrupted while accepting command.", e);
        }
    }
}
