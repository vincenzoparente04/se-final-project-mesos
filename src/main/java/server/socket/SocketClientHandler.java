package server.socket;

import server.core.VirtualView;
import shared.command.*;

import java.io.*;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Dedicated reading thread for one TCP-connected client.
 * <p>
 * Reads lines from the socket in a blocking loop, and places the resulting {@link GameCommand}
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
    private final Consumer<String> onDisconnect;

    public SocketClientHandler(VirtualView virtualView,
                               BufferedReader in,
                               BlockingQueue<GameCommand> commandQueue,
                               Consumer<String> onDisconnect) {
        this.virtualView = virtualView;
        this.in = in;
        this.commandQueue = commandQueue;
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
            deserialize(line);
        } catch (IllegalArgumentException e) {
            virtualView.sendError(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


// ─────────────────────────────────────────────────────────
// Command dispatch
// ─────────────────────────────────────────────────────────

    /**
     * Deserializes the Base64-encoded command and adds it to the queue.
     */
    public void deserialize(String rawMessage) throws InterruptedException {
        try {
            // Decode from Base64
            byte[] decoded = Base64.getDecoder().decode(rawMessage);
            
            // Deserialize to GameCommand
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded));
            GameCommand command = (GameCommand) ois.readObject();
            ois.close();
            
            // Add to queue for processing
            commandQueue.put(command);
        } catch (IllegalArgumentException e) {
            virtualView.sendError("DECODE_ERROR:" + e.getMessage());
        } catch (ClassNotFoundException | ClassCastException e) {
            virtualView.sendError("INVALID_COMMAND:" + e.getMessage());
        } catch (IOException e) {
            virtualView.sendError("SERIALIZATION_ERROR:" + e.getMessage());
        }
    }

}