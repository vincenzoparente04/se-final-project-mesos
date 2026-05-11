package network.server.core;

import shared.command.*;
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

/**
 * Central registry for all client sessions on the server. This class manages three orthogonal concerns:
 * <ul>
 *   <li>handshake for socket connections and registry of the ({@link #connectedPlayers});</li>
 *   <li>lobbies waiting to fill up ({@link #lobbies});</li>
 *   <li>games currently being played ({@link #activeGames}).</li>
 * </ul>
 * It is the single entry point for both transports: socket clients arrive via
 * {@link #openSocketConnection(Socket)}, RMI clients via {@link #addRmiPlayer(RmiPlayerEntry)}.
 * It dispatches the lobby-menu commands ({@code LIST}, {@code CREATE}, {@code JOIN}) using the visitor
 * pattern over {@link LobbyCommand}.
 *
 * <h2>Threading model</h2>
 * One instance is shared by every transport thread (socket acceptor workers,
 * RMI dispatcher threads, individual client handler threads). Every public method
 * that touches the three maps is {@code synchronized}, and the few private helpers
 * ({@code nameAlreadyTaken}, {@code playerAlreadyInLobby}, {@code checkAndStartIfFull})
 * are only ever called from inside synchronized blocks.
 *
 * <h2>I/O outside the lock</h2>
 * Network I/O performed during the socket handshake is deliberately kept
 * outside the synchronized block so that a slow or hostile client cannot block
 * other clients from joining. Only the final registration step takes the lock.
 */
public class LobbyManager implements LobbyCommandVisitor {

    private static final int CONNECT_TIMEOUT_MS = 5_000;

    /** Soglia oltre la quale un player senza heartbeat è considerato disconnesso. */
    private static final long HEARTBEAT_TIMEOUT_MS = 6_000L;
    /** Periodo di scansione della mappa lastHeartbeat. */
    private static final long HEARTBEAT_CHECK_PERIOD_MS = 2_000L;

    // @GuardedBy("this") // TODO controlla che tutti i metodi che accedono a queste siano synchronized
    private final Map<String, Lobby> lobbies = new LinkedHashMap<>(); // <id, lobby>
    private final Map<String, PlayerEntry> connectedPlayers = new HashMap<>(); // <PlayerName, PlayerEntry>
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
            // vedi se questo cast è necessario; in caso forse è meglio instanceOf
            ConnectMessage connect = (ConnectMessage) in.readObject();
            socket.setSoTimeout(0);

            String playerName = connect.playerName();
            SocketVirtualView view = new SocketVirtualView(playerName, socket, out);

            synchronized (this) {

            if (nameAlreadyTaken(playerName)) {
               view.sendError("name_already_taken:" + playerName);
               view.close();
               throw new IOException();
            }

            SocketClientHandler handler = new SocketClientHandler(view, in, this);
            SocketPlayerEntry entry = new SocketPlayerEntry(playerName, in, view, handler);


                // if the just added player has the same name of a player in an active game it reactivates it
                if (activeGames.containsKey(playerName)) { // search between activeGames
                    Game game = activeGames.get(playerName);
                    game.onPlayerReconnected(playerName, entry);
                    connectedPlayers.put(playerName, entry);
                }else {
                    connectedPlayers.put(playerName, entry);
                    view.sendLobbyList(currentLobbyList());
                }

                lastHeartbeat.put(playerName, System.currentTimeMillis());

            Thread t = new Thread(handler, "client-" + playerName);
            t.setDaemon(true);
            t.start();
            }
        } catch (IOException | ClassNotFoundException e) {
            closeSocket(socket);
        }
    }

    /**
     * Registers a new joined RMI player. Symmetric to {@link #openSocketConnection(Socket)}:
     * rejects duplicate names, triggers reconnection if the name belongs to an active game,
     * otherwise adds the player to {@link #connectedPlayers} and primes their lobby
     * list.
     */
    public synchronized void addRmiPlayer(RmiPlayerEntry entry) {
        // if the just added player has the same name of a connected player it returns
        if (nameAlreadyTaken(entry.getName())) {  // search between connectedPlayers
            entry.getView().sendError("name_already_taken:" + entry.getName());
            entry.getView().close();
            return;
        }

        // if the just added player has the same name of a player in an active game it reactivates it
        if (activeGames.containsKey(entry.getName())) { // search between activeGames
            Game game = activeGames.get(entry.getName());
            game.onPlayerReconnected(entry.getName(), entry);
            connectedPlayers.put(entry.getName(), entry);
        }else {
            connectedPlayers.put(entry.getName(), entry);
            entry.getView().sendLobbyList(currentLobbyList());
        }

        lastHeartbeat.put(entry.getName(), System.currentTimeMillis());
    }

    /**
     * Entry point for lobby-menu commands. Dispatches via the visitor
     * pattern to one of the {@code visit(...)} overloads.
     */
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

    @Override
    public synchronized void visit(LeaveCommand cmd) throws Exception {
        String playerName = cmd.getPlayerName();

        if (isPlayerInEndGame(playerName)) {
            handleLeaveFromGame(playerName);
        } else if (isPlayerInLobby(playerName)) {
            handleLeaveFromLobby(playerName);
        } else {
            VirtualView view = getView(playerName);
            if (view != null) {
                view.sendError("LEAVE_INVALID:not_in_lobby_or_game");
            }
        }
    }

    // Game start

    private void checkAndStartIfFull(Lobby lobby) {
        if (!lobby.isFull()) return;

        lobby.getViews().forEach(VirtualView::sendGameStarting);
        lobbies.remove(lobby.getId());

        BlockingQueue<GameCommand> queue = new LinkedBlockingQueue<>();
        List<PlayerEntry> players = lobby.getPlayers();

        Game game = new Game(players, queue);
        game.start();

        players.forEach(p -> activeGames.put(p.getName(), game));
    }

    // Disconnect
    // TODO: controlla che la disconnessione avvenga correttamente
    public synchronized void onDisconnected(String playerName) {
        // rivedere chiamate a onDisconncted, nel frarttempo faccio questo controllo
        if (!connectedPlayers.containsKey(playerName)
                && !lastHeartbeat.containsKey(playerName)) {
            return;
        }

        PlayerEntry entry = connectedPlayers.remove(playerName);
        lastHeartbeat.remove(playerName);

        Game gameSession = activeGames.get(playerName);
        if (gameSession != null) {
            // Game.onPlayerDisconnected chiude già la view di questo player
            // e notifica gli altri.
            gameSession.onPlayerDisconnected(playerName);
        } else { // if the game was not started yet (player was in a lobby)
            // Remove the single player from all lobbies
            lobbies.values().forEach(lobby -> 
                    lobby.getPlayers().stream().filter(p -> p.getName().equals(playerName)).forEach(lobby::removePlayer));
            // Notify remaining players in all lobbies
            lobbies.values().forEach(lobby ->
                    lobby.getViews().forEach(v -> v.sendLobbyList(currentLobbyList())));
            // empty lobbies deleted
            lobbies.values().removeIf(lobby -> lobby.getPlayers().isEmpty());
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

    // ─── LEAVE command handlers ────────────────────────────────────────

    private boolean isPlayerInEndGame(String playerName) {
        return (activeGames.containsKey(playerName) && activeGames.get(playerName).isGameOver());
    }

    private boolean isPlayerInLobby(String playerName) {
        return lobbies.values().stream()
                .flatMap(l -> l.getPlayers().stream())
                .anyMatch(p -> p.getName().equals(playerName));
    }

    private void handleLeaveFromLobby(String playerName) {
        // Find the lobby where the player is currently in
        Lobby lobbyToLeave = lobbies.values().stream()
                .filter(lobby -> lobby.getPlayers().stream().anyMatch(p -> p.getName().equals(playerName)))
                .findFirst()
                .orElse(null);

        try {
            //remove the player
            if (lobbyToLeave != null) {
                lobbyToLeave.getPlayers().stream().filter(p -> p.getName().equals(playerName)).forEach(lobbyToLeave::removePlayer);
                //Notify the left player about active lobbies
                getView(playerName).sendError("Lobby left");
            }


            // Notify remaining players in that lobby of the new state
            if (!lobbyToLeave.getPlayers().isEmpty()) {
                broadcastLobbyState(lobbyToLeave);
            } else {
                lobbies.values().remove(lobbyToLeave);
            }

            // Notify the leaving player with updated lobby list and confirmation
            VirtualView leavingPlayerView = getView(playerName);
            if (leavingPlayerView != null) {
                leavingPlayerView.sendError("LEFT_LOBBY:success");
                leavingPlayerView.sendLobbyList(currentLobbyList());
            }

        } catch (Exception e) {
            if (getView(playerName) != null) {
                getView(playerName).sendError("ERROR_leaving_lobby" + e.getMessage());
            }
        }
    }

    private void handleLeaveFromGame(String playerName) {
        Game game = activeGames.get(playerName);
        if (game != null) {
            VirtualView leavingPlayerView = getView(playerName);
            game.onPlayerLeft(playerName);

            try {
                // If game is now empty, remove ALL references to it from activeGames
                if (game.getActivePlayers().isEmpty()) {
                    activeGames.values().removeIf(g -> g == game);
                }

                // Notify the leaving player with confirmation
                if (leavingPlayerView != null) {
                    leavingPlayerView.sendError("LEFT_GAME:success");
                    leavingPlayerView.sendLobbyList(currentLobbyList());
                }
            } catch (Exception e) {
                if (getView(playerName) != null) {
                    getView(playerName).sendError("ERROR_leaving_lobby" + e.getMessage());
                }
            }
        }
    }

    // Helpers

    private void broadcastLobbyState(Lobby lobby) {
        LobbyDto dto = lobby.toDto();
        lobby.getViews().forEach(v -> v.sendLobbyState(dto));
    }

    private List<LobbyDto> currentLobbyList() {
        return lobbies.values().stream()
                .filter(l -> !l.isFull())
                .map(Lobby::toDto)
                .toList();
    }

    private VirtualView getView(String playerName) {
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
