package server.core;

import server.rmi.RmiPlayerEntry;
import server.socket.PlayerConnection;
import server.socket.SocketPlayerEntry;
import server.socket.SocketVirtualView;
import shared.command.GameCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Accumulates incoming connections — from TCP sockets or RMI — until the
 * expected number of players has joined, then creates and starts a
 * {@link GameSession}.
 * <p>
 * {@link #addSocketClient(Socket)} performs the blocking socket handshake
 * outside the lobby lock so a slow client does not block other lobby joiners.
 * {@link #addRmiPlayer(RmiPlayerEntry)} remains fully {@code synchronized}
 * because RMI calls are already non-blocking from the client's perspective.
 */
public class LobbyManager {

    private static final int CONNECT_TIMEOUT_MS = 5_000;

    private final int expectedPlayers;
    private final BlockingQueue<GameCommand> commandQueue;

    // @GuardedBy("this")
    private final List<PlayerEntry> players = new ArrayList<>();
    // @GuardedBy("this")
    private boolean started = false;

    public LobbyManager(int expectedPlayers, BlockingQueue<GameCommand> commandQueue) {
        this.expectedPlayers = expectedPlayers;
        this.commandQueue = commandQueue;
    }

    // ─────────────────────────────────────────────────────────
    // Socket entry point
    // ─────────────────────────────────────────────────────────

    /**
     * Called by the socket acceptor thread for each new TCP connection.
     * Reads the mandatory {@code CONNECT:playerName} handshake outside the
     * lobby lock — a slow client must not block other lobby joiners.
     * Delegates to {@link #registerSocketPlayer} for the synchronized
     * registration step.
     */
    public void addSocketClient(Socket socket) {
        // handshake socket OUT of lock — a slow client must not block other lobby joiners
        if (started) { rejectSocket(socket, "lobby_full"); return; }
        PlayerConnection conn = openConnection(socket);
        if (conn == null) return;
        SocketVirtualView view = new SocketVirtualView(conn.name(), conn.socket(), conn.out());
        registerSocketPlayer(conn, view);
    }

    // ─────────────────────────────────────────────────────────
    // RMI entry point
    // ─────────────────────────────────────────────────────────

    /**
     * Called by {@link server.rmi.GameServerRemoteImpl} when an RMI client
     * invokes {@code join}.  The {@link RmiPlayerEntry} already wraps an
     * {@link server.rmi.RmiVirtualView} capable of sending lobby notifications.
     */
    public synchronized void addRmiPlayer(RmiPlayerEntry entry) {
        if (started) {
            entry.getView().sendError("lobby_full");
            return;
        }

        if (nameAlreadyTaken(entry.getName())) {
            entry.getView().sendError("name_already_taken:" + entry.getName());
            return;
        }

        registerPlayer(entry);
    }

    // ─────────────────────────────────────────────────────────
    // Common registration
    // ─────────────────────────────────────────────────────────

    private synchronized void registerSocketPlayer(PlayerConnection conn, SocketVirtualView view) {
        if (started) {
            view.sendError("lobby_full");
            view.close();
            return;
        }
        if (nameAlreadyTaken(conn.name())) {
            view.sendError("name_already_taken:" + conn.name());
            view.close();
            return;
        }
        registerPlayer(new SocketPlayerEntry(conn, view));
    }

    private void registerPlayer(PlayerEntry entry) {
        players.add(entry);
        broadcastWaiting();

        if (players.size() == expectedPlayers) {
            started = true;
            new GameSession(players, commandQueue).start();
        }
    }

    private void broadcastWaiting() {
        int current = players.size();
        if (current == expectedPlayers) return;
        players.forEach(p -> p.getView().sendWaiting(current, expectedPlayers));
    }

    // ─────────────────────────────────────────────────────────
    // Socket helpers
    // ─────────────────────────────────────────────────────────

    private PlayerConnection openConnection(Socket socket) {
        try {
            socket.setSoTimeout(CONNECT_TIMEOUT_MS);
            PrintWriter out   = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line = in.readLine();
            socket.setSoTimeout(0);

            if (line != null && line.startsWith("CONNECT:") && line.length() > 8) {
                return new PlayerConnection(line.substring(8).trim(), socket, in, out);
            }
            closeSocket(socket);
        } catch (IOException e) {
            System.err.println("Error reading CONNECT message: " + e.getMessage());
        }
        return null;
    }

    private void rejectSocket(Socket socket, String reason) {
        try (socket; PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("ERROR:" + reason);
        } catch (IOException ignored) {}
    }

    private void closeSocket(Socket socket) {
        try { socket.close(); } catch (IOException ignored) {}
    }

    private boolean nameAlreadyTaken(String name) {
        return players.stream().anyMatch(p -> p.getName().equals(name));
    }
}
