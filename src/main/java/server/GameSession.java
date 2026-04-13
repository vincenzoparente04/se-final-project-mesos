package server;

import controller.GameController;
import model.GameModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Infrastructure container for one game instance.
 * <p>
 * Responsibilities are limited to network lifecycle:
 * <ul>
 *   <li>Creates one {@link VirtualView} and {@link ClientHandler} per player.</li>
 *   <li>Subscribes to the controller's DTO stream and broadcasts each snapshot
 *       to all connected clients — it never builds DTOs itself.</li>
 *   <li>Handles client disconnections by notifying the remaining players
 *       and closing all sockets.</li>
 * </ul>
 * No static state — multiple {@code GameSession} instances can run simultaneously.
 */
public class GameSession {

    private final List<PlayerConnection> connections;
    private final GameController controller;
    private final List<VirtualView> views = new ArrayList<>();
    private final List<ClientHandler> handlers = new ArrayList<>();

    public GameSession(List<PlayerConnection> connections) {
        this.connections = List.copyOf(connections);
        this.controller = new GameController(new GameModel());
    }

    /**
     * Creates one {@link VirtualView} per player, subscribes to the controller's
     * DTO stream so every state change is broadcast to all clients, then
     * starts the game and launches one {@link ClientHandler} thread per player.
     */
    public void start() {
        List<String> playerNames = connections.stream().map(PlayerConnection::name).toList();

        for (PlayerConnection conn : connections) {
            VirtualView view = new VirtualView(controller, conn.out());
            views.add(view);

            ClientHandler handler = new ClientHandler(view, conn.in(), this);
            handlers.add(handler);
        }

        controller.addDtoListener(dto -> views.forEach(v -> v.sendState(dto)));

        controller.startGame(playerNames);

        handlers.forEach(h -> new Thread(h).start());
    }

    /**
     * Called by a {@link ClientHandler} when its client disconnects.
     * Notifies all remaining clients and closes all sockets.
     */
    public synchronized void onClientLeft(ClientHandler handler) {
        int index = handlers.indexOf(handler);
        String name = index >= 0 ? connections.get(index).name() : "unknown";
        views.forEach(v -> v.sendError("player_disconnected:" + name));
        closeSockets();
        // TODO chiudi tutta la partita
    }

    private void closeSockets() {
        connections.forEach(c -> {
            try { c.socket().close(); } catch (IOException ignored) {}
        });
    }
}
