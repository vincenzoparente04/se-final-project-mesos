package network.server.core;

import shared.message.ConnectMessage;
import shared.message.ErrorMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Performs the handshake for a single socket connection, on the per-connection
 * thread spawned by the {@code ServerMain} acceptor (never on the acceptor thread,
 * so a slow or hostile client only stalls itself). <strong>All</strong> blocking
 * socket I/O lives here: opening the streams, reading the {@link ConnectMessage}
 * and writing rejections.
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>create the streams and read a {@link ConnectMessage} (blocking, timeout
 *       {@value #CONNECT_TIMEOUT_MS} ms);</li>
 *   <li>enqueue the registration on the {@code LobbyManager}
 *       ({@code submitSocketRegistration}) and block on the future with a timeout,
 *       receiving only a {@code boolean} verdict;</li>
 *   <li><b>ACCEPT</b> &rarr; return: the view/handler and the reader thread have
 *       already been built and started by the lobby-thread;
 *       <b>REJECT</b> (name taken) &rarr; rewrite the error on the same stream and
 *       read another {@link ConnectMessage} (the socket stays open for the retry);
 *       <b>timeout/error</b> &rarr; write the error and close.</li>
 * </ol>
 */
public class ConnectionHandshaker implements Runnable {

    /** Timeout for reading a {@link ConnectMessage} and for awaiting the registration verdict. */
    private static final int CONNECT_TIMEOUT_MS = 50_000;

    /** The live client connection this handshaker negotiates. */
    private final Socket socket;
    /** Registry the player is submitted to once a name has been read. */
    private final LobbyManager lobbyManager;

    /**
     * @param socket the freshly accepted client connection
     * @param lobbyManager the registry to submit the player to once a name is read
     */
    public ConnectionHandshaker(Socket socket, LobbyManager lobbyManager) {
        this.socket = socket;
        this.lobbyManager = lobbyManager;
    }

    /**
     * Runs the name-negotiation loop: read a {@link ConnectMessage}, submit the
     * registration and block on the verdict, looping on rejection until a name is
     * accepted or the connection times out / fails. On any unrecoverable I/O or
     * deserialization error the socket is closed and the thread ends.
     */
    @Override
    public void run() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Name-negotiation loop: keep reading ConnectMessage attempts until
            // one is accepted (the socket stays open across rejections so the
            // client can retry without re-establishing the transport).
            while (true) {
                try {
                    socket.setSoTimeout(CONNECT_TIMEOUT_MS);
                    ConnectMessage connect = (ConnectMessage) in.readObject();
                    socket.setSoTimeout(0);

                    String playerName = connect.playerName();

                    boolean accepted;
                    try {
                        accepted = lobbyManager.submitSocketRegistration(playerName, socket, in, out)
                                .get(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException | ExecutionException ex) {
                        // The lobby-thread did not answer in time, or building the
                        // session failed: close the connection.
                        trySendError(out, "connection_timeout:registration_failed");
                        closeSocket();
                        return;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        closeSocket();
                        return;
                    }

                    if (accepted) {
                        // Il lobby-thread ha già costruito la sessione e avviato il
                        // reader: l'handshake è concluso.
                        return;
                    }

                    // REJECT: name taken → rewrite the error and read again (loop).
                    sendHandshakeError(out, "name_already_taken:" + playerName);
                } catch (SocketTimeoutException e) {
                    // Timed out while reading the ConnectMessage: send an error and close.
                    trySendError(out, "connection_timeout:no_connect_message_received");
                    closeSocket();
                    return;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            closeSocket();
        }
    }

    /**
     * Writes an {@link ErrorMessage} directly through the handshake's output
     * stream. Used to reject a name attempt without spinning up a full session.
     *
     * @param out the handshake output stream
     * @param message the error text to send
     * @throws IOException if the write fails
     */
    private void sendHandshakeError(ObjectOutputStream out, String message) throws IOException {
        synchronized (out) {
            out.reset();
            out.writeObject(new ErrorMessage(message));
            out.flush();
        }
    }

    /**
     * Best-effort {@link #sendHandshakeError}: swallows any {@link IOException}.
     *
     * @param out the handshake output stream
     * @param message the error text to send
     */
    private void trySendError(ObjectOutputStream out, String message) {
        try {
            sendHandshakeError(out, message);
        } catch (IOException ignored) {}
    }

    /** Quietly closes the socket, ignoring any {@link IOException}. */
    private void closeSocket() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
