package client;

import com.google.gson.Gson;
import shared.dto.GameStateDto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Client-side proxy for the game server.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Sends commands to the server as single text lines over TCP.</li>
 *   <li>Owns the {@link ClientThread} that reads server responses in the
 *       background and calls {@link #onStateReceived} / {@link #onErrorReceived}.</li>
 * </ul>
 * The client never holds a copy of the model: all state comes from
 * the server as {@link GameStateDto} snapshots.
 */
public class VirtualServer {

    private static final Gson GSON = new Gson();

    private final String playerName;
    private final Socket socket;
    private final PrintWriter out;
    private final LocalGameState localState;
    private final ClientStateListener listener;

    public VirtualServer(String host, int port, String playerName,
                         LocalGameState localState, ClientStateListener listener) throws IOException {
        this.playerName = playerName;
        this.localState = localState;
        this.listener = listener;

        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Announce ourselves to the lobby
        out.println("CONNECT:" + playerName);

        // Start the background reading thread
        new Thread(new ClientThread(in, this)).start();
    }

    // ─────────────────────────────────────────────────────────
    // Commands (GUI → server)
    // ─────────────────────────────────────────────────────────

    public void sendChooseColor(String color) {
        out.println("CHOOSE_COLOR:" + playerName + ":" + color);
    }

    public void sendPlaceTotem(char tile) {
        out.println("PLACE_TOTEM:" + playerName + ":" + tile);
    }

    public void sendDrawCard(int cardId) {
        out.println("DRAW_CARD:" + playerName + ":" + cardId);
    }

    public void sendEndTurn() {
        out.println("END_TURN:" + playerName);
    }

    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    // ─────────────────────────────────────────────────────────
    // Callbacks from ClientThread (server → client)
    // ─────────────────────────────────────────────────────────

    void onStateReceived(String json) {
        GameStateDto dto = GSON.fromJson(json, GameStateDto.class);
        localState.update(dto);
        listener.onGameStateUpdated(localState);
    }

    void onWaitingReceived(String raw) {
        listener.onWaiting(raw);
    }

    void onErrorReceived(String message) {
        listener.onError(message);
    }
}
