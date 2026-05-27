package network.server.core;

import controller.GameController;
import database.DatabaseConfig;
import shared.command.*;
import shared.command.lobbyCommand.*;
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

import network.server.rmi.RmiPlayerEntry;
import network.server.socket.SocketClientHandler;
import network.server.socket.SocketPlayerEntry;
import network.server.socket.SocketVirtualView;
import shared.message.ConnectMessage;
import shared.message.ErrorMessage;

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
 * that touches the three maps is {@code synchronized}.
 *
 * <h2>What this class does NOT do</h2>
 * It never reads or writes the game model. When a lifecycle event affects a
 * running session (disconnect, reconnect, leave-from-endgame), the
 * {@code LobbyManager} simply impila il corrispondente {@link LobbyCommand}
 * sulla coda della session e ritorna subito: la mutazione del model è eseguita
 * dal game thread del {@code GameController}, mai dal LobbyManager.
 *
 * <h2>I/O outside the lock</h2>
 * Network I/O performed during the socket handshake is deliberately kept
 * outside the synchronized block so that a slow or hostile client cannot block
 * other clients from joining. Only the final registration step takes the lock.
 */
public class LobbyManager implements LobbyCommandVisitor {

    private static final int CONNECT_TIMEOUT_MS = 50_000;

    private final Map<String, Lobby> lobbies = new LinkedHashMap<>(); // <id, lobby>
    private final Map<String, PlayerEntry> connectedPlayers = new HashMap<>(); // <PlayerName, PlayerEntry>
    /** @apiNote @GuardedBy("this") */
    private final Map<String, GameController> activeGames = new HashMap<>(); // <PlayerName, GameController>


    // Socket entry point ───────────────────────────────────
    public void openSocketConnection(Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Name-negotiation loop: keep reading ConnectMessage attempts until
            // one is accepted (the socket stays open across rejections so the
            // client can retry without re-establishing the transport).
            while (true) {
                try {
                    socket.setSoTimeout(CONNECT_TIMEOUT_MS);
                    ConnectMessage connect = (ConnectMessage) in.readObject();
                    socket.setSoTimeout(0);

                    String playerName = connect.playerName();
                    SocketVirtualView view = new SocketVirtualView(playerName, socket, out, this);

                    synchronized (this) {
                        if (nameAlreadyTaken(playerName)) {
                            // Send the rejection directly through the existing
                            // output stream; do NOT close the socket.
                            sendHandshakeError(out, "name_already_taken:" + playerName);
                            continue;
                        }

                        SocketClientHandler handler = new SocketClientHandler(view, in, this);
                        SocketPlayerEntry entry = new SocketPlayerEntry(playerName, in, view, handler);

                        registerPlayer(playerName, entry);

                        Thread t = new Thread(handler, "client-" + playerName);
                        t.setDaemon(true);
                        t.start();
                    }
                    return;
                } catch (java.net.SocketTimeoutException e) {
                    // Timeout durante la lettura di ConnectMessage: invia errore e chiude
                    try {
                        sendHandshakeError(out, "connection_timeout:no_connect_message_received");
                    } catch (IOException ignored) {}
                    return;  // Chiudi la connessione
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            closeSocket(socket);
        }
    }

    /**
     * Writes an {@link ErrorMessage} directly through the handshake's output
     * stream. Used to reject a name attempt without spinning up a full
     * {@link SocketVirtualView}.
     */
    private void sendHandshakeError(ObjectOutputStream out, String message) throws IOException {
        synchronized (out) {
            out.reset();
            out.writeObject(new ErrorMessage(message));
            out.flush();
        }
    }

    /**
     * Registers a new joined RMI player. Symmetric to {@link #openSocketConnection(Socket)}:
     * rejects duplicate names, triggers reconnection if the name belongs to an active game,
     * otherwise adds the player to {@link #connectedPlayers} and primes their lobby
     * list.
     *
     * @return {@code true} if the player was accepted (or reconnected),
     *         {@code false} if the name was already taken. On rejection the
     *         caller is responsible for any cleanup of the exported callback.
     */
    public synchronized boolean addRmiPlayer(RmiPlayerEntry entry) {
        if (nameAlreadyTaken(entry.getName())) {
            return false;
        }
        registerPlayer(entry.getName(), entry);
        return true;
    }

    /**
     * Common registration path for both socket and RMI: handles the
     * reconnection case (player name already in an active game) and the
     * brand-new connection case. Caller must hold the monitor.
     */
    private void registerPlayer(String playerName, PlayerEntry entry) {
        connectedPlayers.put(playerName, entry);
        entry.getView().activateLiveness();

        GameController controller = activeGames.get(playerName);
        if (controller != null) {
            entry.setGameQueue(controller.getGameCommandQueue());
            enqueue(controller.getQueue(), new PlayerReconnectedCommand(playerName, entry.getView()));
        } else {
            entry.getView().sendLobbyList(currentLobbyList());
        }
    }

    // LobbyCommandVisitor –––––––––––––––––––––––––––––––––––––––––––––

    @Override
    public synchronized void visit(ListLobbiesCommand cmd) {
        VirtualView view = getView(cmd.playerName());
        if (view != null) view.sendLobbyList(currentLobbyList());
    }

    @Override
    public synchronized void visit(CreateLobbyCommand cmd) {
        PlayerEntry entry = connectedPlayers.get(cmd.playerName());
        if (entry == null) return;

        if (activeGames.containsKey(entry.getName())) {
            entry.getView().sendError("already in game");
            return;
        }

        if (cmd.playersNumber() < 2 || cmd.playersNumber() > 5) {
            entry.getView().sendError("invalid player number: must be between 2 and 5");
            return;
        }
  
        if (playerAlreadyInLobby(cmd.playerName())) {
            entry.getView().sendError("already_in_lobby");
            return;
        }

        Lobby lobby = new Lobby(cmd.playerName() + "'s lobby", cmd.playersNumber());
        lobbies.put(lobby.getId(), lobby);
        lobby.addPlayer(entry);
        lobby.broadcastState();
        startIfFull(lobby);
        broadcastLobbyListToBrowsers();
    }

    @Override
    public synchronized void visit(JoinLobbyCommand cmd) {
        PlayerEntry entry = connectedPlayers.get(cmd.playerName());
        if (entry == null) return;

        if (activeGames.containsKey(entry.getName())) {
            entry.getView().sendError("already in game");
            return;
        }

        if (playerAlreadyInLobby(cmd.playerName())) {
            entry.getView().sendError("already_in_lobby");
            return;
        }

        Lobby lobby = lobbies.get(cmd.lobbyId());
        if (lobby == null || lobby.isFull()) {
            entry.getView().sendError("lobby_not_found_or_full");
            broadcastLobbyListToBrowsers();
            return;
        }

        lobby.addPlayer(entry);
        lobby.broadcastState();
        startIfFull(lobby);
    }

    /**
     * Bifurcates a {@link LeaveCommand} based on the player's state:
     * <ul>
     *   <li>player in a lobby (pre-game): the lobby is updated inline (only
     *       state local to the LobbyManager — no model touched);</li>
     *   <li>player in an {@code END_OF_GAME} session: the command is impilato
     *       sulla coda della session, dove il controller pulir&agrave; il model;
     *       il LobbyManager rimuove l'entry da {@code activeGames} e, se non
     *       resta nessun player connesso per quella session, shutta la session;</li>
     *   <li>player non in lobby n&eacute; in partita: errore inline.</li>
     * </ul>
     */

    // TODO da rivedere perché forse è meglio un listener
    @Override
    public synchronized void visit(LeaveCommand cmd) {
        String playerName = cmd.getPlayerName();
        GameController controller = activeGames.get(playerName);

        if (controller != null && controller.isGameOver()) {
            enqueue(controller.getQueue(), cmd);
            activeGames.remove(playerName);

            boolean stillHasConnected = activeGames.entrySet().stream()
                    .filter(e -> e.getValue() == controller)
                    .anyMatch(e -> connectedPlayers.containsKey(e.getKey()));
            if (!stillHasConnected) {
                // L'ultimo player connesso ha lasciato: rimuovi anche eventuali
                // stragglers (player disconnessi che non hanno mai inviato
                // LeaveCommand) e chiudi il controller.
                activeGames.values().removeIf(c -> c == controller);
                controller.shutdown();
            }

            VirtualView leavingView = getView(playerName);
            if (leavingView != null) {
                leavingView.sendError("LEFT_GAME:success");
                leavingView.sendLobbyList(currentLobbyList());
            }
            return;
        }

        if (isPlayerInLobby(playerName)) {
            handleLeaveFromLobby(playerName);
            return;
        }

        VirtualView view = getView(playerName);
        if (view != null) view.sendError("LEAVE_INVALID");
    }

    // Game start

    private void startIfFull(Lobby lobby) {
        if (!lobby.isFull()) return;

        lobby.notifyGameStarting();
        lobbies.remove(lobby.getId());

        List<PlayerEntry> players = lobby.getPlayers();
        GameController controller = new GameController(players);
        controller.start();
        players.forEach(p -> activeGames.put(p.getName(), controller));
    }

    /**
     * Single disconnect entry point. Called by the network endpoints
     * ({@code SocketClientHandler} finally block, {@code RmiVirtualView}
     * send failure) and by the heartbeat scanner. Updates the LobbyManager's
     * local state, closes the outbound view, and — if the player was in a
     * running game — impila un {@link PlayerDisconnectedCommand} sulla coda
     * della session per delegare al controller la pulizia del model.
     */
    public synchronized void onDisconnect(String playerName) {
        if (!connectedPlayers.containsKey(playerName)) return;

        // 1) Impila il PlayerDisconnectedCommand PRIMA di chiudere la view.
        //    Quando questo metodo viene invocato dal thread del sentinel
        //    server-side (callback onTimeoutAction), il successivo
        //    view.close() chiama sentinel.stop() → executor.shutdownNow()
        //    che invoca Thread.interrupt() sul thread corrente: una
        //    queue.put() chiamata dopo verrebbe rifiutata immediatamente
        //    con InterruptedException, perdendo l'evento e impedendo al
        //    game thread di attivare la logica di disconnect / sospensione.
        GameController controller = activeGames.get(playerName);
        if (controller != null) {
            enqueue(controller.getQueue(), new PlayerDisconnectedCommand(playerName));
        }

        // 2) Rimuovi il player dal registry e chiudi la view. Da qui in poi
        //    il flag interrupt del thread chiamante può essere settato dal
        //    sentinel.stop() interno a view.close(): le operazioni
        //    successive (HashMap.remove, lookup lobby) non sono
        //    interrompibili quindi proseguono regolarmente.
        PlayerEntry entry = connectedPlayers.remove(playerName);
        if (entry != null) entry.getView().close();

        // 3) Era in lobby (o solo connesso, non in nessuna lobby): trova la
        //    lobby specifica, rimuovi il player, notifica solo quella lobby.
        Lobby hostingLobby = lobbies.values().stream()
                .filter(l -> l.containsPlayer(playerName))
                .findFirst()
                .orElse(null);

        if (hostingLobby == null) {
            // player era solo "browsing" → niente da fare lato lobby
            return;
        }
        hostingLobby.removePlayerByName(playerName);
        if (hostingLobby.isEmpty()) {
            lobbies.remove(hostingLobby.getId());
        }
        broadcastLobbyListToBrowsers();
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
        Lobby lobbyToLeave = lobbies.values().stream()
                .filter(l -> l.containsPlayer(playerName))
                .findFirst()
                .orElse(null);

        VirtualView leavingView = getView(playerName);
        if (lobbyToLeave == null) {
            if (leavingView != null) leavingView.sendError("LEAVE_INVALID:not_in_lobby");
            return;
        }

        lobbyToLeave.removePlayerByName(playerName);
        if (lobbyToLeave.isEmpty()) {
            lobbies.remove(lobbyToLeave.getId());
        }

        if (leavingView != null) {
            leavingView.sendError("LEFT_LOBBY:success");
            leavingView.sendLobbyList(currentLobbyList());
        }
        broadcastLobbyListToBrowsers();
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
        return lobbies.values().stream().anyMatch(l -> l.containsPlayer(playerName));
    }

    /**
     * Manda la lista corrente delle lobby aperte ai soli player in stato
     * "browsing". Da invocare ogni volta che la lista lobby visibile cambia.
     */
    private void broadcastLobbyListToBrowsers() {
        List<LobbyDto> list = currentLobbyList();
        browsingPlayers().forEach(e -> e.getView().sendLobbyList(list));
    }

    /**
     * Ritorna gli {@link PlayerEntry} dei player attualmente in stato "browsing":
     * connessi, non in nessuna lobby, non in nessuna partita attiva.
     */
    private List<PlayerEntry> browsingPlayers() {
        return connectedPlayers.values().stream()
                .filter(e -> !activeGames.containsKey(e.getName()))
                .filter(e -> lobbies.values().stream().noneMatch(l -> l.containsPlayer(e.getName())))
                .toList();
    }

    private void closeSocket(Socket socket) {
        try { socket.close(); } catch (IOException ignored) {}
    }

    /**
     * Helper to put a command on a session queue without leaking the
     * {@code InterruptedException}: restore the interrupt flag and proceed.
     * Used for the lifecycle commands the LobbyManager impila.
     */
    private static void enqueue(BlockingQueue<ClientCommand> queue, ClientCommand cmd) {
        try {
            queue.put(cmd);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ─── Ordered shutdown ──────────────────────────────────────────────

    /**
     * Stop the heartbeat scanner, drain every active game session, close all
     * outbound views and clear every internal map. After this call returns
     * the LobbyManager is no longer usable and the JVM can exit cleanly.
     *
     * <p>Invoked from a JVM shutdown hook in {@code ServerMain}.
     */
    public synchronized void shutdown() {
        new java.util.HashSet<>(activeGames.values()).forEach(GameController::shutdown);
        connectedPlayers.values().forEach(e -> e.getView().close());
        lobbies.clear();
        activeGames.clear();
        connectedPlayers.clear();
    }
}
