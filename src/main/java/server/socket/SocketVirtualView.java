package server.socket;

import com.google.gson.Gson;
import server.core.VirtualView;
import shared.dto.GameStateDto;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * {@link VirtualView} implementation for TCP socket connections.
 * <p>
 * Serialises {@link GameStateDto} to JSON and writes it to the client's
 * {@link PrintWriter} as text lines.  All sends are synchronous — the
 * {@link server.core.GameSession} broadcast executor ensures that one slow
 * socket never stalls the rest.
 */
public class SocketVirtualView implements VirtualView {

    private static final Gson GSON = new Gson();

    private final String playerName;
    private final Socket socket;
    private final PrintWriter out;

    public SocketVirtualView(String playerName, Socket socket, PrintWriter out) {
        this.playerName = playerName;
        this.socket = socket;
        this.out = out;
    }

    @Override
    public void sendState(GameStateDto dto) {
        out.println("STATE:" + GSON.toJson(dto));
        if (dto.winners != null && !dto.winners.isEmpty()) {
            out.println("GAME_OVER:" + String.join(",", dto.winners));
        }
    }

    @Override
    public void sendError(String message) {
        out.println("ERROR:" + message);
    }

    @Override
    public void sendWaiting(int current, int expected) {
        out.println("WAITING:" + current + ":" + expected);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
