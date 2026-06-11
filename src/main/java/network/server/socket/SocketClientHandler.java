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
 * Reader for a single socket client, run on the per-player {@code client-<name>}
 * thread. It blocks reading {@link ClientCommand}s and dispatches each to its
 * destination: lobby commands to the {@link LobbyManager}, in-game commands to the
 * player's game-session queue, and heartbeats to the liveness watchdog. When the
 * stream ends or fails it triggers the disconnect pipeline via
 * {@link LobbyManager#onDisconnect}.
 */
public class SocketClientHandler implements Runnable {

    /** Outbound view, used to report errors and to refresh inbound liveness. */
    private final VirtualView virtualView;
    /** Inbound command stream from this client. */
    private final ObjectInputStream in;
    /** Registry to forward lobby commands and disconnects to. */
    private final LobbyManager lobbyManager;
    /** In-game command queue; wired once the player joins a game, {@code null} otherwise. */
    private volatile BlockingQueue<GameCommand> gameQueue = null;

    /** Routes each inbound {@link ClientCommand} by type (lobby / in-game / heartbeat). */
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
            // Isolated liveness channel: only HeartbeatCommand refreshes the watchdog.
            virtualView.notifyInbound();
        }
    };

    /**
     * @param virtualView the outbound view for this client
     * @param in the inbound command stream
     * @param lobbyManager the registry for lobby commands and disconnects
     */
    public SocketClientHandler(VirtualView virtualView, ObjectInputStream in, LobbyManager lobbyManager) {
        this.virtualView = virtualView;
        this.in = in;
        this.lobbyManager = lobbyManager;
    }

    /**
     * Wires the in-game command queue once the player enters a game session.
     *
     * @param queue the game session's command queue
     */
    public void setGameQueue(BlockingQueue<GameCommand> queue) {
        this.gameQueue = queue;
    }

    /**
     * Read loop: dispatch each {@link ClientCommand} until the stream closes or
     * fails, then trigger the disconnect pipeline in the {@code finally} block.
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
     * Dispatches one command, restoring the interrupt flag on
     * {@link InterruptedException} and reporting any other failure back to the
     * client as an error message.
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
