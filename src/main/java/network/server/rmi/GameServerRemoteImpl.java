package network.server.rmi;

import shared.command.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import network.client.rmi.ClientCallbackRemote;
import network.server.core.LobbyManager;
import shared.command.gameCommand.GameCommand;
import shared.command.lobbyCommand.HeartbeatCommand;
import shared.command.lobbyCommand.LobbyCommand;

public class GameServerRemoteImpl extends UnicastRemoteObject implements GameServerRemote {

    private final LobbyManager lobbyManager;
    private final ConcurrentHashMap<String, BlockingQueue<GameCommand>> gameQueues = new ConcurrentHashMap<>();

    private final ClientCommandVisitor dispatcher = new ClientCommandVisitor() {
        @Override
        public void visit(LobbyCommand cmd) throws Exception {
            cmd.accept(lobbyManager);
        }

        @Override
        public void visit(GameCommand cmd) throws InterruptedException {
            BlockingQueue<GameCommand> queue = gameQueues.get(cmd.getPlayerName());
            if (queue == null) return;
            queue.put(cmd);
        }

        @Override
        public void visit(HeartbeatCommand cmd) {
            lobbyManager.onHeartbeatReceived(cmd.playerName());
        }
    };

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
        boolean connectionSuccessful = lobbyManager.addRmiPlayer(new RmiPlayerEntry(view, this));
        if (connectionSuccessful) {
            System.out.println("RMI connection from " + host);
        }
        return connectionSuccessful;
    }

    @Override
    public void submitClientCommand(ClientCommand command) throws RemoteException {
        try {
            command.accept(dispatcher);
        } catch (Exception e) {
            throw new RemoteException("Command routing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect(String playerName) throws RemoteException {
        lobbyManager.onDisconnect(playerName);
    }
}
