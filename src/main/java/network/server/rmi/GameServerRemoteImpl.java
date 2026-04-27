package network.server.rmi;

import shared.command.ClientCommand;
import shared.command.CommandDispatcher;
import shared.command.GameCommand;
import shared.command.LobbyCommand;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import network.client.rmi.ClientCallbackRemote;
import network.server.core.LobbyManager;

public class GameServerRemoteImpl extends UnicastRemoteObject implements GameServerRemote {

    private final LobbyManager lobbyManager;
    private final ConcurrentHashMap<String, BlockingQueue<GameCommand>> gameQueues = new ConcurrentHashMap<>();

    private final CommandDispatcher dispatcher = new CommandDispatcher() {
        @Override
        public void onLobbyCommand(LobbyCommand cmd) throws Exception {
            lobbyManager.handle(cmd);
        }

        @Override
        public void onGameCommand(GameCommand cmd) throws InterruptedException {
            BlockingQueue<GameCommand> queue = gameQueues.get(cmd.getPlayerName());
            if (queue == null) {
                return;
            }
            queue.put(cmd);
        }
    };

    public GameServerRemoteImpl(LobbyManager lobbyManager) throws RemoteException {
        super();
        this.lobbyManager = lobbyManager;
    }

    public void registerQueue(String playerName, BlockingQueue<GameCommand> queue) {
        gameQueues.put(playerName, queue);
    }

    @Override
    public void join(String playerName, ClientCallbackRemote callback) throws RemoteException {
        RmiVirtualView view = new RmiVirtualView(playerName, callback);
        lobbyManager.addRmiPlayer(new RmiPlayerEntry(view, q -> registerQueue(playerName, q), lobbyManager));
    }

    @Override
    public void submitClientCommand(ClientCommand command) throws RemoteException {
        try {
            command.accept(dispatcher);
        } catch (Exception e) {
            throw new RemoteException("Command routing failed: " + e.getMessage(), e);
        }
    }
}
