package client;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Background thread that reads lines from the server socket and
 * forwards them to {@link VirtualServer}.
 * <p>
 * This class has no dependency on JavaFX: callbacks are invoked directly
 * on the reading thread.  If a JavaFX UI needs to update after the callback,
 * the {@link ClientStateListener} implementation should wrap its code in
 * {@code Platform.runLater()} — not this class.
 */
public class ClientThread implements Runnable {

    private static final String STATE_PREFIX   = "STATE:";
    private static final String ERROR_PREFIX   = "ERROR:";
    private static final String WAITING_PREFIX = "WAITING:";

    private final BufferedReader in;
    private final VirtualServer virtualServer;

    public ClientThread(BufferedReader in, VirtualServer virtualServer) {
        this.in = in;
        this.virtualServer = virtualServer;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                dispatch(line);
            }
        } catch (IOException ignored) {
            // connection closed
        }
    }

    private void dispatch(String line) {
        if (line.startsWith(STATE_PREFIX)) {
            virtualServer.onStateReceived(line.substring(STATE_PREFIX.length()));
        } else if (line.startsWith(ERROR_PREFIX)) {
            virtualServer.onErrorReceived(line.substring(ERROR_PREFIX.length()));
        } else if (line.startsWith(WAITING_PREFIX)) {
            virtualServer.onWaitingReceived(line.substring(WAITING_PREFIX.length()));
        }
    }
}
