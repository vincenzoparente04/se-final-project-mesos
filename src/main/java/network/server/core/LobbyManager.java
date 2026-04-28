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

//TODO controlla che tutti i metodi che accedono a queste siano synchronized
    private final Map<String, Lobby> lobbies = new LinkedHashMap<>();
    // @GuardedBy("this")
    private final Map<String, PlayerEntry> connectedPlayers = new HashMap<>();
    // @GuardedBy("this")
    private final Map<String, Game> activeGames = new HashMap<>();

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
        if (nameAlreadyTaken(entry.getName())) {
            entry.getView().sendError("name_already_taken:" + entry.getName());
            return;
        }
        connectedPlayers.put(entry.getName(), entry);
        entry.getView().sendLobbyList(currentLobbyList());
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
        Game gameSession = activeGames.remove(playerName);
        if (gameSession != null) {
            gameSession.onPlayerDisconnected(playerName);
        } else {
            lobbies.values().forEach(lobby ->
                    lobby.getViews().forEach(v -> v.sendLobbyList(currentLobbyList())));
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
