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

    private final Map<String, Lobby> lobbies = new LinkedHashMap<>(); // <id, lobby>
    private final Map<String, PlayerEntry> connectedPlayers = new HashMap<>(); // <PlayerName, PlayerEntry>
    private final Map<String, Game> activeGames = new HashMap<>(); // <PlayerName, Game>

    /**
     * Central registry for all client sessions on the server. This class manages three orthogonal concerns:
     * <ul>
     *   <li>connected players that have completed the handshake but are not yet
     *       in any lobby or game ({@link #connectedPlayers});</li>
     *   <li>lobbies waiting to fill up ({@link #lobbies});</li>
     *   <li>games currently being played ({@link #activeGames}).</li>
     * </ul>
     * It is the single entry point for both transports — socket clients arrive
     * via {@link #openSocketConnection(Socket)}, RMI clients via
     * {@link #addRmiPlayer(RmiPlayerEntry)} — and dispatches the lobby-menu
     * commands ({@code LIST}, {@code CREATE}, {@code JOIN}) using the visitor
     * pattern over {@link LobbyCommand}.
     *
     * <h2>Threading model</h2>
     * One instance is shared by every transport thread (socket acceptor workers,
     * RMI dispatcher threads, individual client handler threads). All access to
     * the three maps is serialised on the instance monitor: every public method
     * that touches them is {@code synchronized}, and the few private helpers
     * ({@code nameAlreadyTaken}, {@code playerAlreadyInLobby},
     * {@code checkAndStartIfFull}) are only ever called from inside a
     * synchronized region.
     *
     * <h2>I/O outside the lock</h2>
     * Network I/O performed during the socket handshake is deliberately kept
     * outside the synchronized block so that a slow or hostile client cannot
     * block other clients from joining. Only the final registration step takes
     * the lock.
     */
    public void openSocketConnection(Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            socket.setSoTimeout(CONNECT_TIMEOUT_MS);
            // vedi se questo cast è necessario
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

        // TODO: controlla che se il nome è gia preso tutto viene chiuso correttamente

            return;
        }

        // if the just added player has the same name of a player in an active game it reactivates it
        if (activeGames.containsKey(entry.getName())) { // search between activeGames
            Game game = activeGames.get(entry.getName());
            game.onPlayerReconnected(entry.getName(), entry);
        }
        connectedPlayers.put(entry.getName(), entry);
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

        Game game = new Game(players, queue);
        game.start();

        players.forEach(p -> activeGames.put(p.getName(), game));
    }

    // Disconnect
    // TODO: controlla che la disconnessione avvenga correttamente
    public synchronized void onDisconnected(String playerName) {
        connectedPlayers.remove(playerName);
        Game game = activeGames.get(playerName); // returns the Game the player was in, or null if not in any
        if (game != null) {
            game.onPlayerDisconnected(playerName);
        } else { // if the game was not started yet (player was in a lobby)
            lobbies.values().removeIf(lobby -> lobby.getPlayers().stream().anyMatch(p -> p.getName().equals(playerName))); // delete player from the lobby
            lobbies.values().forEach(lobby -> lobby.getViews().forEach(v -> v.sendLobbyList(currentLobbyList())));
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

// TODO:    - non c'è metodo removeGame quando finisce una partita
//          - la lobby non deve essere eliminata se un solo player la abbandona
