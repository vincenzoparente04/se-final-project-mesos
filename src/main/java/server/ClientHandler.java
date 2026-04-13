package server;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Dedicated reading thread for one connected client.
 * <p>
 * Reads lines from the socket in a blocking loop and forwards each line to
 * {@link VirtualView#dispatch(String)}.  On disconnect or I/O error the
 * {@link GameSession} is notified so it can inform the other players.
 */
public class ClientHandler implements Runnable {

    private final VirtualView virtualView;
    private final BufferedReader in;
    private final GameSession session;

    public ClientHandler(VirtualView virtualView, BufferedReader in, GameSession session) {
        this.virtualView = virtualView;
        this.in = in;
        this.session = session;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                virtualView.dispatch(line.trim());
            }
        } catch (IOException ignored) {
            // client closed the connection abruptly
        } finally {
            session.onClientLeft(this);
        }
    }
}
