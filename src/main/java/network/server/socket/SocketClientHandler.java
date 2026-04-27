package network.server.socket;

import shared.command.ClientCommand;
import shared.command.CommandDispatcher;
import shared.command.GameCommand;
import shared.command.LobbyCommand;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

import network.server.core.LobbyManager;
import network.server.core.VirtualView;

public class SocketClientHandler implements Runnable {

    private final VirtualView virtualView;
    private final ObjectInputStream in;
    private final LobbyManager lobbyManager;
    private volatile BlockingQueue<GameCommand> gameQueue = null;

    private final CommandDispatcher dispatcher = new CommandDispatcher() {
        @Override
        public void onLobbyCommand(LobbyCommand cmd) throws Exception {
            lobbyManager.handle(cmd);
        }

        @Override
        public void onGameCommand(GameCommand cmd) throws InterruptedException {
            BlockingQueue<GameCommand> queue = gameQueue;
            if (queue == null) {
                virtualView.sendError("not_in_game");
                return;
            }
            queue.put(cmd);
        }
    };

    public SocketClientHandler(VirtualView virtualView, ObjectInputStream in,
                               LobbyManager lobbyManager) {
        this.virtualView = virtualView;
        this.in = in;
        this.lobbyManager = lobbyManager;
    }

    public void setGameQueue(BlockingQueue<GameCommand> queue) {
        this.gameQueue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                ClientCommand cmd = (ClientCommand) in.readObject();
                handleCommand(cmd);
            }
        } catch (EOFException | SocketException ignored) {
        } catch (IOException | ClassNotFoundException ignored) {
        } finally {
            lobbyManager.onDisconnected(virtualView.getPlayerName());
        }
    }

    private void handleCommand(ClientCommand cmd) {
        try {
            cmd.accept(dispatcher);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            virtualView.sendError(e.getMessage());
        }
    }
}
