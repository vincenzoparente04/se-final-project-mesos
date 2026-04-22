package client.socket;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Background thread that reads lines from the server socket and
 * forwards them to {@link SocketVirtualServer}.
 * <p>
 * This class has no dependency on JavaFX: callbacks are invoked directly
 * on the reading thread.  If a JavaFX UI needs to update after the callback,
 * the {@link client.ClientStateListener} implementation should wrap its code
 * in {@code Platform.runLater()} — not this class.
 */
public class SocketClientThread implements Runnable {

    private static final String STATE_PREFIX     = "STATE:";
    private static final String ERROR_PREFIX     = "ERROR:";
    private static final String WAITING_PREFIX   = "WAITING:";
    private static final String GAME_OVER_PREFIX = "GAME_OVER:";

    private final BufferedReader in;
    private final SocketVirtualServer owner;

    public SocketClientThread(BufferedReader in, SocketVirtualServer owner) {
        this.in = in;
        this.owner = owner;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                dispatch(line);
            }
        } catch (IOException ignored) {
            // connection closed abruptly
        } finally {
            owner.onDisconnected();
        }
    }

    private void dispatch(String line) {
        if (line.startsWith(STATE_PREFIX)) {
            owner.onStateReceived(line.substring(STATE_PREFIX.length()));
        } else if (line.startsWith(GAME_OVER_PREFIX)) {
            owner.onGameOverReceived(line.substring(GAME_OVER_PREFIX.length()));
        } else if (line.startsWith(ERROR_PREFIX)) {
            owner.onErrorReceived(line.substring(ERROR_PREFIX.length()));
        } else if (line.startsWith(WAITING_PREFIX)) {
            owner.onWaitingReceived(line.substring(WAITING_PREFIX.length()));
        }
    }
}
