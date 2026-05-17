package network.server.socket;

import shared.command.ClientCommand;
import shared.command.CommandDispatcher;
import shared.command.GameCommand;
import shared.command.LobbyCommand;
import shared.command.HeartbeatCommand;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

import network.server.core.LobbyManager;

public class SocketClientHandler implements Runnable {

    // Tipizzato come SocketVirtualView (non VirtualView) per accedere
    // al metodo package-private notifyInbound() senza cast.
    private final SocketVirtualView virtualView;
    private final ObjectInputStream in;
    private final LobbyManager lobbyManager;
    private volatile BlockingQueue<GameCommand> gameQueue = null;

    private final CommandDispatcher dispatcher = new CommandDispatcher() {
        @Override
        public void onLobbyCommand(LobbyCommand cmd) throws Exception {
            cmd.accept(lobbyManager);
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

        @Override
        public void onHeartbeatCommand(HeartbeatCommand cmd) {
            // Canale di liveness isolato: solo HeartbeatCommand aggiorna il watchdog.
            virtualView.notifyInbound();
        }
    };

    public SocketClientHandler(SocketVirtualView virtualView, ObjectInputStream in, LobbyManager lobbyManager) {
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
            lobbyManager.onDisconnect(virtualView.getPlayerName());
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
