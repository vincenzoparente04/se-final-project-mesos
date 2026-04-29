package network.server.core;

import shared.command.LobbyCommandVisitor;
import shared.command.GameCommand;
import shared.command.CreateLobbyCommand;
import shared.command.JoinLobbyCommand;
import shared.command.ListLobbiesCommand;
import shared.command.LobbyCommand;
import shared.dto.LobbyDto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import network.server.rmi.RmiPlayerEntry;
import network.server.socket.SocketClientHandler;
import network.server.socket.SocketPlayerEntry;
import network.server.socket.SocketVirtualView;
import shared.message.ConnectMessage;

public class LobbyManager implements LobbyCommandVisitor {

    private static final int CONNECT_TIMEOUT_MS = 5_000;

    /** Soglia oltre la quale un player senza heartbeat è considerato disconnesso. */
    private static final long HEARTBEAT_TIMEOUT_MS = 6_000L;
    /** Periodo di scansione della mappa lastHeartbeat. */
    private static final long HEARTBEAT_CHECK_PERIOD_MS = 2_000L;

    // @GuardedBy("this") // TODO controlla che tutti i metodi che accedono a queste siano synchronized
    private final Map<String, Lobby> lobbies = new LinkedHashMap<>(); // <id, lobby>
    // @GuardedBy("this")
    private final Map<String, PlayerEntry> connectedPlayers = new HashMap<>(); // <PlayerName, PlayerEntry>
    // @GuardedBy("this")
    private final Map<String, Game> activeGames = new HashMap<>(); // <PlayerName, Game>


    /**
     * Timestamp (millis epoch) dell'ultimo heartbeat ricevuto da ciascun player.
     * ConcurrentHashMap perché viene letta dallo scheduler e scritta dai
     * thread di rete (SocketClientHandler / RMI) senza il lock {@code this}.
     */
    private final java.util.concurrent.ConcurrentHashMap<String, Long> lastHeartbeat =
            new java.util.concurrent.ConcurrentHashMap<>();

    private final java.util.concurrent.ScheduledExecutorService heartbeatScanner =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "heartbeat-scanner");
                t.setDaemon(true);
                return t;
            });

    public LobbyManager() {
        heartbeatScanner.scheduleAtFixedRate(
                this::scanForDeadConnections,
                HEARTBEAT_CHECK_PERIOD_MS,
                HEARTBEAT_CHECK_PERIOD_MS,
                java.util.concurrent.TimeUnit.MILLISECONDS);
    }


    // ─── Socket entry point ───────────────────────────────────

    public void openSocketConnection(Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            socket.setSoTimeout(CONNECT_TIMEOUT_MS);
            ConnectMessage connect = (ConnectMessage) in.readObject();
            socket.setSoTimeout(0);

            String playerName = connect.playerName();
            SocketVirtualView view = new SocketVirtualView(playerName, socket, out);
            SocketClientHandler handler = new SocketClientHandler(view, in, this);
            SocketPlayerEntry entry = new SocketPlayerEntry(playerName, in, view, handler);

            synchronized (this) {
                if (nameAlreadyTaken(playerName)) {
                    view.sendError("name_already_taken:" + playerName);
                    view.close();
                    return;
                }

                // if the just added player has the same name of a player in an active game it reactivates it
                if (activeGames.containsKey(playerName)) { // search between activeGames
                    Game game = activeGames.get(playerName);
                    game.onPlayerReconnected(playerName, entry);
                }
                connectedPlayers.put(playerName, entry);
                lastHeartbeat.put(playerName, System.currentTimeMillis());

            }

            Thread t = new Thread(handler, "client-" + playerName);
            t.setDaemon(true);
            t.start();

            view.sendLobbyList(currentLobbyList());

        } catch (IOException | ClassNotFoundException e) {
            closeSocket(socket);
        }
    }

    public synchronized void addRmiPlayer(RmiPlayerEntry entry) {
        // if the just added player has the same name of a connected player it returns
        if (nameAlreadyTaken(entry.getName())) {  // search between connectedPlayers
            entry.getView().sendError("name_already_taken:" + entry.getName());
            return;
        }

        // if the just added player has the same name of a player in an active game it reactivates it
        if (activeGames.containsKey(entry.getName())) { // search between activeGames
            Game game = activeGames.get(entry.getName());
            game.onPlayerReconnected(entry.getName(), entry);
        }
        connectedPlayers.put(entry.getName(), entry);
        lastHeartbeat.put(entry.getName(), System.currentTimeMillis());
        //entry.getView().sendLobbyList(currentLobbyList());
    }

    // Entry point for lobbies commands
    public synchronized void handle(LobbyCommand cmd) throws Exception {
        cmd.accept(this);
    }

    // LobbyCommandVisitor

    @Override
    public synchronized void visit(ListLobbiesCommand cmd) {
        VirtualView view = getView(cmd.playerName());
        if (view != null) view.sendLobbyList(currentLobbyList());
    }

    @Override
    public synchronized void visit(CreateLobbyCommand cmd) {
        PlayerEntry entry = connectedPlayers.get(cmd.playerName());
        if (entry == null) return;
        // TODO forse controllo inutile
        if (playerAlreadyInLobby(cmd.playerName())) {
            entry.getView().sendError("already_in_lobby");
            return;
        }

        Lobby lobby = new Lobby(cmd.playerName() + "'s lobby", cmd.maxPlayers());
        lobbies.put(lobby.getId(), lobby);
        lobby.addPlayer(entry);
        broadcastLobbyState(lobby);
        checkAndStartIfFull(lobby);
    }

    @Override
    public synchronized void visit(JoinLobbyCommand cmd) {
        PlayerEntry entry = connectedPlayers.get(cmd.playerName());
        if (entry == null) return;
        if (playerAlreadyInLobby(cmd.playerName())) {
            entry.getView().sendError("already_in_lobby");
            return;
        }

        Lobby lobby = lobbies.get(cmd.lobbyId());
        if (lobby == null || lobby.isFull()) {
            entry.getView().sendError("lobby_not_found_or_full");
            return;
        }

        lobby.addPlayer(entry);
        broadcastLobbyState(lobby);
        checkAndStartIfFull(lobby);
    }

    // Game start

    private void checkAndStartIfFull(Lobby lobby) {
        if (!lobby.isFull()) return;

        lobby.getViews().forEach(VirtualView::sendGameStarting);
        lobbies.remove(lobby.getId());

        BlockingQueue<GameCommand> queue = new LinkedBlockingQueue<>();
        List<PlayerEntry> players = lobby.getPlayers();

        Game gameSession = new Game(players, queue);
        gameSession.start();

        players.forEach(p -> activeGames.put(p.getName(), gameSession));
    }

    // Disconnect
    public synchronized void onDisconnected(String playerName) {
        connectedPlayers.remove(playerName);
        lastHeartbeat.remove(playerName);

        Game gameSession = activeGames.get(playerName); // returns the Game the player was in, or null if not in any
        if (gameSession != null) {
            gameSession.onPlayerDisconnected(playerName);
        } else { // if the game was not started yet (player was in a lobby)
            lobbies.values().removeIf(lobby -> lobby.getPlayers().stream().anyMatch(p -> p.getName().equals(playerName))); // delete player from the lobby
            lobbies.values().forEach(lobby ->
                    lobby.getViews().forEach(v -> v.sendLobbyList(currentLobbyList())));
        }
    }


    /**
     * Aggiorna il timestamp dell'ultimo heartbeat per il player.
     * Invocato dai thread di rete quando arriva un {@link shared.command.HeartbeatCommand}.
     * <p>
     * Non è {@code synchronized} perché opera solo sulla {@code ConcurrentHashMap}
     * {@link #lastHeartbeat}; non tocca lo stato protetto da {@code this}.
     */
    public void onHeartbeatReceived(String playerName) {
        // computeIfPresent invece di put: se un heartbeat ritardato arriva DOPO
        // che il player è stato rimosso, non lo "resuscita" come zombie.
        lastHeartbeat.computeIfPresent(playerName, (k, v) -> System.currentTimeMillis());
    }

    /**
     * Scansione periodica: identifica i player il cui ultimo heartbeat
     * è più vecchio di {@link #HEARTBEAT_TIMEOUT_MS} e ne forza la disconnessione
     * attraverso la pipeline esistente {@link #onDisconnected(String)}.
     */
    private void scanForDeadConnections() {
        long now = System.currentTimeMillis();
        // Snapshot per non iterare la mappa mentre la modifichiamo via onDisconnected.
        List<String> dead = lastHeartbeat.entrySet().stream()
                .filter(e -> now - e.getValue() > HEARTBEAT_TIMEOUT_MS)
                .map(Map.Entry::getKey)
                .toList();
        for (String playerName : dead) {
            onDisconnected(playerName);
        }
    }

    // Helpers

    private void broadcastLobbyState(Lobby lobby) {
        LobbyDto dto = lobby.toDto();
        lobby.getViews().forEach(v -> v.sendLobbyState(dto));
    }

    private synchronized List<LobbyDto> currentLobbyList() {
        return lobbies.values().stream()
                .filter(l -> !l.isFull())
                .map(Lobby::toDto)
                .toList();
    }

    private synchronized VirtualView getView(String playerName) {
        PlayerEntry entry = connectedPlayers.get(playerName);
        return entry != null ? entry.getView() : null;
    }

    private boolean nameAlreadyTaken(String name) {
        return connectedPlayers.containsKey(name);
    }

    private boolean playerAlreadyInLobby(String playerName) {
        return lobbies.values().stream()
                .flatMap(l -> l.getPlayers().stream())
                .anyMatch(p -> p.getName().equals(playerName));
    }

    private void closeSocket(Socket socket) {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
