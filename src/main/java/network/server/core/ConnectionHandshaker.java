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
 * Performs the handshake for a single incoming socket connection, running on
 * the per-connection thread spawned by the {@link ServerMain} acceptor loop
 * (never on the acceptor thread itself, so a slow or hostile client blocks
 * only itself). All blocking socket I/O lives here: stream creation, reading
 * the initial {@link ConnectMessage}, and writing rejections.
 *
 * The name-negotiation loop keeps the socket open across rejections so the
 * client can retry without reconnecting. The flow is: read a
 * {@link ConnectMessage} (timeout {@value #CONNECT_TIMEOUT_MS} ms), submit
 * the registration to {@link LobbyManager} and block on the future verdict.
 * On ACCEPT the method returns — the lobby thread has already built the
 * session and started the reader. On REJECT it sends an error and loops back.
 * On timeout or I/O error it sends an error and closes the socket.
 */
public class ConnectionHandshaker implements Runnable {

    private static final int CONNECT_TIMEOUT_MS = 50_000;

    private final Socket socket;
    private final LobbyManager lobbyManager;

    /**
     * @param socket       the accepted client socket
     * @param lobbyManager the lobby manager to submit the registration to
     */
    public ConnectionHandshaker(Socket socket, LobbyManager lobbyManager) {
        this.socket = socket;
        this.lobbyManager = lobbyManager;
    }

    /**
     * Executes the name-negotiation loop. Reads {@link ConnectMessage} objects
     * and submits each one to {@link LobbyManager} until one is accepted, the
     * connection times out, or an I/O error occurs.
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
                        // The lobby thread did not respond in time or session setup failed: close.
                        trySendError(out, "connection_timeout:registration_failed");
                        closeSocket();
                        return;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        closeSocket();
                        return;
                    }

                    if (accepted) {
                        // The lobby thread has already built the session and started the reader.
                        return;
                    }

                    // REJECT: name already taken — send an error and loop back.
                    sendHandshakeError(out, "name_already_taken:" + playerName);
                } catch (SocketTimeoutException e) {
                    // Timeout waiting for ConnectMessage: send an error and close.
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
     */
    private void sendHandshakeError(ObjectOutputStream out, String message) throws IOException {
        synchronized (out) {
            out.reset();
            out.writeObject(new ErrorMessage(message));
            out.flush();
        }
    }

    /**
     * Best-effort variant of {@link #sendHandshakeError}: swallows any
     * {@link IOException} so it is safe to call during error-recovery paths.
     */
    private void trySendError(ObjectOutputStream out, String message) {
        try {
            sendHandshakeError(out, message);
        } catch (IOException ignored) {}
    }

    /** Closes the socket, ignoring any {@link IOException}. */
    private void closeSocket() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
