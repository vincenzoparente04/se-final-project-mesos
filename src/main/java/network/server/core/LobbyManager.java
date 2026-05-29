package network.server.core;

import controller.GameController;
import network.server.socket.SocketClientHandler;
import network.server.socket.SocketPlayerEntry;
import network.server.socket.SocketVirtualView;
import shared.command.*;
import shared.command.lobbyCommand.*;
import shared.dto.LobbyDto;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Central registry for all client sessions on the server. This class manages three orthogonal concerns:
 * <ul>
 *   <li>registry of the connected players ({@link #connectedPlayers});</li>
 *   <li>lobbies waiting to fill up ({@link #lobbies});</li>
 *   <li>games currently being played ({@link #activeGames}).</li>
 * </ul>
 *
 * <h2>Threading model — coda/attore (mirror del {@link GameController})</h2>
 * Il {@code LobbyManager} è un attore: possiede una singola
 * {@code BlockingQueue<LobbyCommand>} e un unico {@code lobby-thread} che la
 * consuma in {@link #run()} con {@code cmd.accept(this)}. <strong>Tutte</strong>
 * le mutazioni delle tre mappe avvengono su questo thread, quindi le mappe sono
 * {@code HashMap}/{@code LinkedHashMap} semplici senza alcun lock: la
 * serializzazione della coda È la mutua esclusione.
 * <p>
 * Gli endpoint di rete (RMI dispatcher, socket handler, sentinel di liveness)
 * non chiamano mai i {@code visit(...)} direttamente: impilano i comandi via
 * {@link #submit(LobbyCommand)} / {@link #onDisconnect(String)} (pattern
 * "tell", fire-and-forget). Le sole operazioni sincrone sono le registrazioni
 * ({@link #submitSocketRegistration}/{@link #submitRmiRegistration}), che usano
 * il pattern "ask": il chiamante blocca su un {@link CompletableFuture} che il
 * lobby-thread completa con il verdetto ACCEPT/REJECT. Il lobby-thread non
 * blocca mai: il check-e-registra del nome è un lookup O(1) atomico, eliminando
 * la race TOCTOU sui nomi duplicati.
 *
 * <h2>Niente I/O sul lobby-thread</h2>
 * L'I/O di handshake socket vive nel {@link ConnectionHandshaker} (sul thread
 * per-connessione). Le sole attese bloccanti residue ({@code view.close()} con
 * il drain del sender, {@code controller.shutdown()} con la {@code join} del
 * game-thread) sono delegate a un piccolo executor {@code lobby-io}, così che
 * il lobby-thread torni subito a servire la coda.
 *
 * <h2>What this class does NOT do</h2>
 * It never reads or writes the game model. When a lifecycle event affects a
 * running session (disconnect, reconnect, leave-from-endgame), il
 * {@code LobbyManager} impila il corrispondente {@link LobbyCommand} sulla coda
 * della session e ritorna subito: la mutazione del model è eseguita dal game
 * thread del {@code GameController}, mai dal LobbyManager.
 */
public class LobbyManager implements Runnable, LobbyCommandVisitor {

    private final Map<String, Lobby> lobbies = new LinkedHashMap<>(); // <id, lobby>
    private final Map<String, PlayerEntry> connectedPlayers = new HashMap<>(); // <PlayerName, PlayerEntry>
    private final Map<String, GameController> activeGames = new HashMap<>(); // <PlayerName, GameController>

    private final BlockingQueue<LobbyCommand> queue = new LinkedBlockingQueue<>();
    private final Thread lobbyThread;
    private volatile boolean running = true;

    /**
     * Executor a thread singolo per le sole attese di terminazione di thread
     * locali ({@code view.close()}, {@code controller.shutdown()}): attese di
     * thread propri, senza rete né lock altrui, ma che fermerebbero la coda se
     * eseguite sul lobby-thread.
     */
    private final ExecutorService lobbyIo = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lobby-io");
        t.setDaemon(true);
        return t;
    });

    public LobbyManager() {
        this.lobbyThread = new Thread(this, "lobby-thread");
        this.lobbyThread.setDaemon(true);
    }

    /** Avvia il lobby-thread. Da chiamare una volta dopo la costruzione. */
    public void start() {
        lobbyThread.start();
    }

    // ─── API pubblica enqueue-only ────────────────────────────────────────

    /** Impila un comando di menu (fire-and-forget). Usata dai dispatcher di rete. */
    public void submit(LobbyCommand cmd) {
        try {
            queue.put(cmd);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Punto d'ingresso unico di disconnessione, invocato dagli endpoint di rete
     * e dai sentinel di liveness. Ora si limita a impilare un
     * {@link LobbyDisconnectCommand}: il corpo gira sul lobby-thread, quindi non
     * serve più l'hack sull'ordine "enqueue-prima-di-close" che evitava
     * l'interrupt del thread del sentinel.
     */
    public void onDisconnect(String playerName) {
        submit(new LobbyDisconnectCommand(playerName));
    }

    /**
     * Registrazione socket "ask": impila la richiesta e ritorna il future che il
     * lobby-thread completerà con il verdetto. La view/handler/entry e il thread
     * reader sono costruiti dal lobby-thread <strong>solo</strong> se il nome è
     * libero (vedi {@link #visit(RegisterSocketPlayerCommand)}) → niente leak del
     * sender executor su REJECT, e socket lasciato aperto per il retry.
     *
     * @return future completato con {@code true} (ACCEPT, reader già avviato) o
     *         {@code false} (REJECT, nome occupato)
     */
    public CompletableFuture<Boolean> submitSocketRegistration(String playerName, Socket socket, ObjectInputStream in, ObjectOutputStream out) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        submit(new RegisterSocketPlayerCommand(playerName, socket, in, out, future));
        return future;
    }

    /**
     * Registrazione RMI "ask". L'{@link PlayerEntry} è già costruito dal
     * {@code GameServerRemoteImpl}; il future veicola il verdetto.
     */
    public CompletableFuture<Boolean> submitRmiRegistration(PlayerEntry entry) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        submit(new RegisterRmiPlayerCommand(entry.getName(), entry, future));
        return future;
    }

    // ─── Lobby-thread loop ────────────────────────────────────────────────

    @Override
    public void run() {
        while (running) {
            try {
                LobbyCommand cmd = queue.take();
                cmd.accept(this);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // A handler-level exception must never kill the lobby thread.
                System.err.println("[lobby-thread] unexpected error: " + e);
            }
        }
    }

    // ─── Registrazioni (sul lobby-thread) ─────────────────────────────────

    /**
     * Check-e-registra atomico per il socket. Se il nome è libero costruisce
     * view/handler/entry e avvia il thread reader (tutte operazioni non
     * bloccanti: {@code Thread.start()} ritorna subito), poi completa il future.
     * La costruzione avviene <strong>dopo</strong> il check, così un nome
     * rifiutato non alloca alcuna {@link SocketVirtualView}. Il future è
     * completato <strong>sempre</strong> (anche su eccezione di costruzione),
     * così che l'handshaker non resti bloccato fino al timeout.
     */
    @Override
    public void visit(RegisterSocketPlayerCommand cmd) {
        String name = cmd.playerName();
        CompletableFuture<Boolean> future = cmd.future();
        try {
            if (nameAlreadyTaken(name)) {
                future.complete(false);
                return;
            }
            SocketVirtualView view = new SocketVirtualView(name, cmd.socket(), cmd.out(), this);
            SocketClientHandler handler = new SocketClientHandler(view, cmd.in(), this);
            SocketPlayerEntry entry = new SocketPlayerEntry(name, cmd.in(), view, handler);

            registerPlayer(name, entry);

            Thread t = new Thread(handler, "client-" + name);
            t.setDaemon(true);
            t.start();

            future.complete(true);
        } catch (RuntimeException e) {
            future.completeExceptionally(e);
        }
    }

    /**
     * Check-e-registra atomico per l'RMI. L'{@link PlayerEntry} è già costruito
     * dal {@code GameServerRemoteImpl} e non c'è alcun thread reader da avviare
     * (il dispatch RMI è guidato dal runtime), quindi qui basta registrare.
     */
    @Override
    public void visit(RegisterRmiPlayerCommand cmd) {
        CompletableFuture<Boolean> future = cmd.future();
        try {
            if (nameAlreadyTaken(cmd.playerName())) {
                future.complete(false);
                return;
            }
            registerPlayer(cmd.playerName(), cmd.entry());
            future.complete(true);
        } catch (RuntimeException e) {
            future.completeExceptionally(e);
        }
    }

    /**
     * Common registration path for both socket and RMI: handles the
     * reconnection case (player name already in an active game) and the
     * brand-new connection case. Runs only on the lobby-thread.
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

    // ─── LobbyCommandVisitor: comandi di menu ─────────────────────────────

    @Override
    public void visit(ListLobbiesCommand cmd) {
        VirtualView view = getView(cmd.playerName());
        if (view != null) view.sendLobbyList(currentLobbyList());
    }

    @Override
    public void visit(CreateLobbyCommand cmd) {
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
    public void visit(JoinLobbyCommand cmd) {
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
        broadcastLobbyListToBrowsers();
    }

    /**
     * Bifurcates a {@link LeaveCommand} based on the player's state:
     * <ul>
     *   <li>player in a lobby (pre-game): the lobby is updated inline (only
     *       state local to the LobbyManager — no model touched);</li>
     *   <li>player in an {@code END_OF_GAME} session: the command is impilato
     *       sulla coda della session, dove il controller pulir&agrave; il model;
     *       il LobbyManager rimuove l'entry da {@code activeGames} e, se non
     *       resta nessun player connesso per quella session, shutta la session
     *       (delegando la {@code join} bloccante a {@code lobby-io});</li>
     *   <li>player non in lobby n&eacute; in partita: errore inline.</li>
     * </ul>
     */
    @Override
    public void visit(LeaveCommand cmd) {
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
                // LeaveCommand) e chiudi il controller fuori dal lobby-thread.
                activeGames.values().removeIf(c -> c == controller);
                lobbyIo.submit(controller::shutdown);
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

    /**
     * Pipeline di disconnessione, eseguita sul lobby-thread. Se il player era in
     * partita impila un {@link PlayerDisconnectedCommand} sulla coda della
     * session; rimuove l'entry e chiude la view (close bloccante delegato a
     * {@code lobby-io}); aggiorna l'eventuale lobby ospitante.
     */
    @Override
    public void visit(LobbyDisconnectCommand cmd) {
        String playerName = cmd.playerName();
        if (!connectedPlayers.containsKey(playerName)) return;

        GameController controller = activeGames.get(playerName);
        if (controller != null) {
            enqueue(controller.getQueue(), new PlayerDisconnectedCommand(playerName));
        }

        PlayerEntry entry = connectedPlayers.remove(playerName);
        if (entry != null) {
            VirtualView view = entry.getView();
            lobbyIo.submit(view::close);
        }

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

    // ─── Game start ───────────────────────────────────────────────────────

    private void startIfFull(Lobby lobby) {
        if (!lobby.isFull()) return;

        lobby.notifyGameStarting();
        lobbies.remove(lobby.getId());

        List<PlayerEntry> players = lobby.getPlayers();
        GameController controller = new GameController(players);
        controller.start();
        players.forEach(p -> activeGames.put(p.getName(), controller));
    }

    // ─── LEAVE command handlers ─────────────────────────────────────────────

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
     * Stop the lobby thread, drain every active game session, close all
     * outbound views and clear every internal map. After this call returns
     * the LobbyManager is no longer usable and the JVM can exit cleanly.
     *
     * <p>Invocato da uno shutdown hook in {@code ServerMain} (thread diverso dal
     * lobby-thread). Ferma e {@code join}a prima il lobby-thread così che da qui
     * in poi questo thread sia il solo ad accedere alle mappe (nessuna race).
     */
    public void shutdown() {
        running = false;
        lobbyThread.interrupt();
        try {
            lobbyThread.join(2000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        new HashSet<>(activeGames.values()).forEach(GameController::shutdown);
        connectedPlayers.values().forEach(e -> e.getView().close());
        lobbies.clear();
        activeGames.clear();
        connectedPlayers.clear();
        lobbyIo.shutdownNow();
    }
}
