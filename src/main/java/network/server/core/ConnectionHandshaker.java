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
 * Esegue l'handshake di una singola connessione socket, sul thread per-connessione
 * spawnato dall'acceptor di {@code ServerMain} (mai sul thread acceptor: un client
 * lento/ostile blocca solo se stesso). Qui vive <strong>tutto</strong> l'I/O socket
 * bloccante (apertura stream, lettura del {@link ConnectMessage}, scrittura del
 * rifiuto).
 *
 * <h2>Flusso</h2>
 * <ol>
 *   <li>crea gli stream e legge un {@link ConnectMessage} (bloccante, timeout
 *       {@value #CONNECT_TIMEOUT_MS} ms);</li>
 *   <li>impila la registrazione sul {@code LobbyManager}
 *       ({@code submitSocketRegistration}) e blocca sul future con timeout,
 *       ricevendo solo un {@code boolean};</li>
 *   <li><b>ACCEPT</b> → ritorna: la view/handler e il thread reader sono già
 *       stati creati e avviati dal lobby-thread;
 *       <b>REJECT</b> (nome occupato) → riscrive l'errore sullo stesso stream e
 *       rilegge un nuovo {@code ConnectMessage} (il socket resta aperto per il retry);
 *       <b>timeout/errore</b> → scrive l'errore e chiude.</li>
 * </ol>
 */
public class ConnectionHandshaker implements Runnable {

    private static final int CONNECT_TIMEOUT_MS = 50_000;

    private final Socket socket;
    private final LobbyManager lobbyManager;

    public ConnectionHandshaker(Socket socket, LobbyManager lobbyManager) {
        this.socket = socket;
        this.lobbyManager = lobbyManager;
    }

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
                        // Il lobby-thread non ha risposto in tempo o la costruzione
                        // della sessione è fallita: chiudi la connessione.
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

                    // REJECT: nome occupato → riscrive l'errore e rilegge (loop).
                    sendHandshakeError(out, "name_already_taken:" + playerName);
                } catch (SocketTimeoutException e) {
                    // Timeout durante la lettura di ConnectMessage: invia errore e chiude.
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

    private void trySendError(ObjectOutputStream out, String message) {
        try {
            sendHandshakeError(out, message);
        } catch (IOException ignored) {}
    }

    private void closeSocket() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
