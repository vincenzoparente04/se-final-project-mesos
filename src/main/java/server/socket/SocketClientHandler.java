package server.socket;

import server.core.VirtualView;
import shared.command.GameCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Dedicated reading thread for one TCP-connected client.
 * <p>
 * Reads lines from the socket in a blocking loop, delegates parsing to
 * {@link SocketCommandParser}, and places the resulting {@link GameCommand}
 * on the shared {@link BlockingQueue} for sequential processing by
 * {@link server.core.GameThread}.
 * <p>
 * Parse errors (unknown command, malformed arguments) are returned to the
 * client as {@code ERROR:} messages via the player's {@link VirtualView}.
 * On disconnect or I/O error the {@code onDisconnect} callback is invoked
 * with the player's name so that {@link server.core.GameSession} can notify
 * the remaining players.
 */
public class SocketClientHandler implements Runnable {

    private final VirtualView virtualView;
    private final BufferedReader in;
    private final BlockingQueue<GameCommand> commandQueue;
    private final SocketCommandParser commandParser;
    private final Consumer<String> onDisconnect;

    public SocketClientHandler(VirtualView virtualView,
                               BufferedReader in,
                               BlockingQueue<GameCommand> commandQueue,
                               SocketCommandParser commandParser,
                               Consumer<String> onDisconnect) {
        this.virtualView = virtualView;
        this.in = in;
        this.commandQueue = commandQueue;
        this.commandParser = commandParser;
        this.onDisconnect = onDisconnect;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleLine(line.trim());
            }
        } catch (IOException ignored) {
            // client closed connection abruptly
        } finally {
            onDisconnect.accept(virtualView.getPlayerName());
        }
    }

    private void handleLine(String line) {
        try {
            GameCommand command = commandParser.parse(line);
            commandQueue.put(command);
        } catch (IllegalArgumentException e) {
            virtualView.sendError(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
