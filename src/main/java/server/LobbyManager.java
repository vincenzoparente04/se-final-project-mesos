package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates incoming connections until the expected number of players
 * has joined, then creates and starts a {@link GameSession}.
 * <p>
 * The {@link BufferedReader} created here to read the {@code CONNECT} message
 * is stored in the {@link PlayerConnection} and reused by {@link GameSession},
 * so no bytes are lost due to a second reader buffering ahead.
 * <p>
 * {@link #addClient(Socket)} is {@code synchronized} because multiple
 * accept-threads could call it concurrently in a multi-lobby extension.
 */
public class LobbyManager {

    private final int expectedPlayers;
    private final List<PlayerConnection> connections = new ArrayList<>();

    public LobbyManager(int expectedPlayers) {
        this.expectedPlayers = expectedPlayers;
    }

    public synchronized void addClient(Socket socket) {
        // TODO: controlla che non si connettano più players
        
        PlayerConnection conn = openConnection(socket);
        if (conn == null) return;

        if (nameAlreadyTaken(conn.name())) {
            reject(conn, "name_already_taken:" + conn.name());
            return;
        }

        connections.add(conn);
        broadcastWaiting();

        if (connections.size() == expectedPlayers) {
            new GameSession(connections).start();
        }
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    /**
     * Opens I/O streams on the socket and reads the mandatory first line
     * {@code CONNECT:playerName}. Returns {@code null} on any failure.
     */
    private PlayerConnection openConnection(Socket socket) {
        try {
            PrintWriter out   = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line = in.readLine();
            if (line != null && line.startsWith("CONNECT:") && line.length() > 8) {
                String name = line.substring(8).trim();
                return new PlayerConnection(name, socket, in, out);
            }
            socket.close();
        } catch (IOException e) {
            System.err.println("Error reading CONNECT message: " + e.getMessage());
        }
        return null;
    }

    private boolean nameAlreadyTaken(String name) {
        return connections.stream().anyMatch(c -> c.name().equals(name));
    }

    private void reject(PlayerConnection conn, String reason) {
        conn.out().println("ERROR:" + reason);
        try { conn.socket().close(); } catch (IOException ignored) {}
    }

    private void broadcastWaiting() {
        int current = connections.size();
        if (current == expectedPlayers) return;
        connections.forEach(c -> c.out().println("WAITING:" + current + ":" + expectedPlayers));
    }
}
