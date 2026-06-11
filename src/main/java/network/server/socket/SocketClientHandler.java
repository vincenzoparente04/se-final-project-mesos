package network.server.socket;

import network.server.core.VirtualView;
import shared.command.ClientCommand;
import shared.command.ClientCommandVisitor;
import shared.command.gameCommand.GameCommand;
import shared.command.lobbyCommand.LobbyCommand;
import shared.command.lobbyCommand.HeartbeatCommand;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

import network.server.core.LobbyManager;

/**
 * Reads {@link ClientCommand} objects from the socket input stream and
 * dispatches them to the correct server-side destination.
 *
 * Runs on a dedicated thread (one per connected client). Incoming commands
 * are routed via the internal {@link ClientCommandVisitor}: {@link LobbyCommand}
 * objects are forwarded to {@link LobbyManager}; {@link GameCommand} objects
 * are placed on the active {@link #gameQueue}; {@link HeartbeatCommand} objects
 * notify the liveness sentinel without entering any queue.
 *
 * When the read loop terminates (EOF, {@link SocketException}, or any I/O
 * error) it invokes {@link LobbyManager#onDisconnect} in its {@code finally}
 * block to trigger the disconnection pipeline.
 *
 * @see SocketPlayerEntry
 * @see SocketVirtualView
 */
public class SocketClientHandler implements Runnable {

    // notifyInbound() is declared in the VirtualView interface.
    private final VirtualView virtualView;
    private final ObjectInputStream in;
    private final LobbyManager lobbyManager;
    private volatile BlockingQueue<GameCommand> gameQueue = null;

    /**
     * Routes each incoming command to its destination: lobby commands go to
     * {@link LobbyManager}, game commands are placed on {@link #gameQueue},
     * and heartbeat commands notify the liveness sentinel.
     */
    private final ClientCommandVisitor dispatcher = new ClientCommandVisitor() {
        @Override
        public void visit(LobbyCommand cmd) {
            lobbyManager.submit(cmd);
        }

        @Override
        public void visit(GameCommand cmd) throws InterruptedException {
            BlockingQueue<GameCommand> queue = gameQueue;
            if (queue == null) {
                virtualView.sendError("not_in_game");
                return;
            }
            queue.put(cmd);
        }

        @Override
        public void visit(HeartbeatCommand cmd) {
            // Isolated liveness channel: only HeartbeatCommand updates the watchdog.
            virtualView.notifyInbound();
        }
    };

    /**
     * @param virtualView  the view used to send error messages back to the client
     * @param in           the object input stream to read commands from
     * @param lobbyManager the lobby manager to forward lobby commands and disconnect events to
     */
    public SocketClientHandler(VirtualView virtualView, ObjectInputStream in, LobbyManager lobbyManager) {
        this.virtualView = virtualView;
        this.in = in;
        this.lobbyManager = lobbyManager;
    }

    public void setGameQueue(BlockingQueue<GameCommand> queue) {
        this.gameQueue = queue;
    }

    /**
     * Read loop: deserialises one {@link ClientCommand} at a time and passes
     * it to {@link #handleCommand}. Terminates silently on EOF or socket
     * error, then triggers the disconnect pipeline via
     * {@link LobbyManager#onDisconnect}.
     */
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

    /**
     * Dispatches a single command through the {@link #dispatcher}.
     * Restores the interrupt flag on {@link InterruptedException}; routes
     * any other exception back to the client as an error message.
     *
     * @param cmd the command to dispatch
     */
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
