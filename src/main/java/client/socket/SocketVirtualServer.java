package client.socket;

import client.ClientStateListener;
import client.LocalGameState;
import client.VirtualServer;
import com.google.gson.Gson;
import shared.dto.GameStateDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link VirtualServer} implementation that communicates with the server over TCP.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Sends commands to the server as single text lines over TCP.</li>
 *   <li>Owns the {@link SocketClientThread} that reads server responses in the
 *       background and calls {@link #onStateReceived} / {@link #onErrorReceived}.</li>
 * </ul>
 * The client never holds a copy of the model: all state comes from
 * the server as {@link GameStateDto} snapshots.
 * <p>
 * {@link #close()} is idempotent and safe to call from any thread.
 */
public class SocketVirtualServer implements VirtualServer {

    private static final Gson GSON = new Gson();

    private final String playerName;
    private final Socket socket;
    private final PrintWriter out;
    private final LocalGameState localState;
    private final ClientStateListener listener;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SocketVirtualServer(String host, int port, String playerName,
                             LocalGameState localState, ClientStateListener listener)
            throws IOException {
        this.playerName = playerName;
        this.localState = localState;
        this.listener = listener;

        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Announce ourselves to the lobby
        out.println("CONNECT:" + playerName);

        // Start the background reading thread
        new Thread(new SocketClientThread(in, this), "socket-reader-" + playerName).start();
    }

    // ─────────────────────────────────────────────────────────
    // Commands (GUI → server)
    // ─────────────────────────────────────────────────────────

    @Override
    public void sendChooseColor(String color) {
        if (closed.get()) return;
        out.println("CHOOSE_COLOR:" + playerName + ":" + color);
    }

    @Override
    public void sendPlaceTotem(char tile) {
        if (closed.get()) return;
        out.println("PLACE_TOTEM:" + playerName + ":" + tile);
    }

    @Override
    public void sendDrawCard(int cardId) {
        if (closed.get()) return;
        out.println("DRAW_CARD:" + playerName + ":" + cardId);
    }

    @Override
    public void sendEndTurn() {
        if (closed.get()) return;
        out.println("END_TURN:" + playerName);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────
    // Callbacks from ClientThread (server → client)
    // ─────────────────────────────────────────────────────────

    void onStateReceived(String json) {
        GameStateDto dto = GSON.fromJson(json, GameStateDto.class);
        localState.update(dto);
        listener.onGameStateUpdated(localState);
    }

    void onGameOverReceived(String raw) {
        List<String> winners = raw.isBlank()
                ? Collections.emptyList()
                : List.of(raw.split(","));
        listener.onGameOver(winners);
    }

    void onWaitingReceived(String raw) {
        listener.onWaiting(raw);
    }

    void onErrorReceived(String message) {
        listener.onError(message);
    }

    void onDisconnected() {
        closed.set(true);
        listener.onDisconnected();
    }
}
