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
 * Central registry and manager for every client session on the server.
 * It owns, and is the sole mutator of, three maps:
 * <ul>
 *   <li>{@link #connectedPlayers} — every connected player, keyed by name;</li>
 *   <li>{@link #lobbies} — lobbies still waiting to fill up;</li>
 *   <li>{@link #activeGames} — players in a running game, each mapped to its
 *       {@link GameController}.</li>
 * </ul>
 *
 * <h2>Threading model — single-thread actor (mirror of {@link GameController})</h2>
 * The {@code LobbyManager} owns one {@link BlockingQueue} of {@link LobbyCommand}
 * and one daemon {@code lobby-thread} that drains it in {@link #run()} via
 * {@code cmd.accept(this)}. Every map mutation happens on that thread, so the maps
 * are plain {@code HashMap}/{@code LinkedHashMap} with no locks. The sole exception is
 * {@link #shutdown()}, which first joins the lobby-thread so it becomes the only
 * accessor before clearing the maps.
 * <p>
 * Network endpoints never invoke the {@code visit(...)} handlers directly; they
 * enqueue commands through an API — {@link #submit(LobbyCommand)} for menu
 * commands and {@link #onDisconnect(String)} for disconnections — and return at
 * once. The callers are {@link network.server.rmi.GameServerRemoteImpl} and
 * {@link SocketClientHandler} (incoming commands) and the per-player
 * {@link shared.liveness.LivenessSentinel}s (liveness timeouts).
 * <p>
 * Player registration is the only synchronous path and uses an "ask" pattern:
 * {@link #submitSocketRegistration} / {@link #submitRmiRegistration} enqueue a
 * registration command and return a {@link CompletableFuture} that the
 * lobby-thread completes with the verdict (accept/reject). The duplicate-name
 * check ({@link #nameAlreadyTaken}) and the reservation
 * ({@code connectedPlayers.put}) run back-to-back on the lobby-thread with no
 * blocking call between them, and each command is handled
 * run-to-completion before the next is dequeued: the queue's serialization
 * <em>is</em> the mutual exclusion. A second attempt on the same name therefore
 * always observes the first one's reservation, so two attempts can never both
 * win.
 * <p>
 *
 * <h2>No blocking work on the lobby-thread</h2>
 * Blocking socket-handshake I/O lives in {@link ConnectionHandshaker}, on the
 * per-connection thread. The only residual blocking calls — {@code view.close()}
 * (drains a player's sender) and {@code controller.shutdown()} (joins a
 * game-thread) — are handed off to a dedicated single-thread executor
 * ({@code shutdown-executor}) so the lobby-thread returns to the queue immediately.
 *
 * It never reads or writes the game model. When a lifecycle event affects a
 * running session (disconnect, reconnect, leave-after-game-over) it enqueues the
 * matching {@link LobbyCommand} on that session's queue and returns; the model is
 * mutated only by the {@link GameController}'s game-thread.
 */
public class LobbyManager implements Runnable, LobbyCommandVisitor {

    /** Open lobbies waiting to fill, keyed by lobby id. Insertion-ordered for stable listings. */
    private final Map<String, Lobby> lobbies = new LinkedHashMap<>();

    /** Every connected player, keyed by player name (the server-wide identity). */
    private final Map<String, PlayerEntry> connectedPlayers = new HashMap<>();

    /** Players in a running game, each mapped to the owning {@link GameController}. */
    private final Map<String, GameController> activeGames = new HashMap<>();

    /** Inbound command queue; the lobby-thread is its only consumer. */
    private final BlockingQueue<LobbyCommand> queue = new LinkedBlockingQueue<>();

    /** The single consumer thread; created in the constructor, started by {@link #start()}. */
    private final Thread lobbyThread;

    /** Cleared by {@link #shutdown()} to end the {@link #run()} loop. */
    private volatile boolean running = true;

    /**
     * Off-loads the only blocking calls the lobby-thread would otherwise make:
     * {@code VirtualView.close()} (drains a player's sender) and
     * {@code GameController.shutdown()} (joins a game-thread). Each is a wait on a
     * local thread — no network, no foreign lock — but running it inline would
     * stall the command queue, so it is delegated here. Single-thread, daemon,
     * named {@code shutdown-executor}.
     */
    private final ExecutorService shutdownExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "shutdown-executor");
        t.setDaemon(true);
        return t;
    });

    /** Creates the lobby-thread; call {@link #start()} to run it. */
    public LobbyManager() {
        this.lobbyThread = new Thread(this, "lobby-thread");
        this.lobbyThread.setDaemon(true);
    }

    /** Starts the lobby-thread. Call once, after construction. */
    public void start() {
        lobbyThread.start();
    }

    // ─── Enqueue-only public API ──────────────────────────────────────────

    /**
     * Enqueues a command for the lobby-thread (fire-and-forget). Used by the
     * network dispatchers to forward menu commands (list/create/join/leave). A
     * pending interrupt is preserved rather than propagated.
     *
     * @param cmd the command to enqueue for the lobby-thread to process
     */
    public void submit(LobbyCommand cmd) {
        try {
            queue.put(cmd);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Single disconnect entry point, called by the transport handlers and the
     * per-player liveness sentinels. It only enqueues a
     * {@link LobbyDisconnectCommand}; the teardown runs later on the lobby-thread
     * ({@link #visit(LobbyDisconnectCommand)}).
     *
     * @param playerName the name of the player whose connection dropped
     */
    public void onDisconnect(String playerName) {
        submit(new LobbyDisconnectCommand(playerName));
    }

    /**
     * Submits a socket player for registration on the queue ("ask" pattern) and returns a
     * future carrying the verdict. The view, handler and reader thread are built by
     * the lobby-thread <strong>only if the name is free</strong> (see
     * {@link #visit(RegisterSocketPlayerCommand)}), so a rejected attempt allocates
     * nothing.
     *
     * @param playerName the requested name
     * @param socket the live client socket (used to build the view if accepted)
     * @param in the handshake input stream, reused as the command reader
     * @param out the handshake output stream, reused for outbound messages
     * @return a future completed with {@code true} (accepted, reader already
     *         started) or {@code false} (rejected, name already taken)
     */
    public CompletableFuture<Boolean> submitSocketRegistration(String playerName, Socket socket, ObjectInputStream in, ObjectOutputStream out) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        submit(new RegisterSocketPlayerCommand(playerName, socket, in, out, future));
        return future;
    }

    /**
     * Submits an already-built RMI {@link PlayerEntry} for registration ("ask"
     * pattern). This path is leaner than the socket one: the entry and its
     * {@code RmiVirtualView} are already constructed by the caller, so the
     * lobby-thread has no view/handler to build and no {@code client-<name>} reader
     * thread to start (RMI dispatch is driven by the runtime, not by a reader loop).
     * There is also no blocking handshake I/O to off-load to the
     * {@code shutdown-executor}, and the {@code RmiVirtualView} allocates no network
     * resources until {@code activateLiveness()}, so a rejected name leaks nothing.
     * The lobby-thread's work thus collapses to the name check plus a single map
     * {@code put} (or a reject).
     *
     * @param entry the RMI player entry created by {@code GameServerRemoteImpl}
     * @return a future completed with {@code true} (accepted) or {@code false}
     *         (rejected, name already taken)
     */
    public CompletableFuture<Boolean> submitRmiRegistration(PlayerEntry entry) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        submit(new RegisterRmiPlayerCommand(entry.getName(), entry, future));
        return future;
    }

    // ─── Lobby-thread loop ────────────────────────────────────────────────

    /**
     * Lobby-thread loop: take one command and dispatch it via {@code accept(this)}.
     * A handler exception is logged but never kills the thread; an interrupt ends
     * the loop cleanly. Mirrors {@link GameController#run()}.
     */
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

    // ─── Registration (on the lobby-thread) ───────────────────────────────

    /**
     * Atomic check-and-register for a socket player. If the name is free it builds
     * the view, handler and entry, registers the player and starts the
     * {@code client-<name>} reader thread, then completes the future with
     * {@code true}; otherwise it completes with {@code false}. All of this is
     * non-blocking and the lobby-thread performs no I/O.
     * Construction happens only after the name check, so a rejected
     * name allocates no instances. The future is
     * <strong>always</strong> completed — even if construction throws — so the
     * handshaker never blocks until its timeout.
     *
     * @param cmd the socket-registration request: requested name, the live socket
     *            and its streams, and the verdict future to complete
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
     * Atomic check-and-register for an RMI player. The {@link PlayerEntry} is
     * already built and RMI has no reader thread, so this only registers the player
     * (or rejects a duplicate name) and completes the future. As above, the future
     * is always completed.
     *
     * @param cmd the RMI-registration request: requested name, the prebuilt
     *            {@link PlayerEntry}, and the verdict future to complete
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
     * Common registration path for both transports: on reconnection (name already
     * in an active game) it wires the player's game queue and enqueues a
     * {@link PlayerReconnectedCommand}; on a new connection it sends the player's
     * lobby list. Runs only on the lobby-thread.
     *
     * @param playerName the player's server-wide identity (the map key)
     * @param entry the freshly built entry to register under {@code playerName}
     */
    private void registerPlayer(String playerName, PlayerEntry entry) {
        connectedPlayers.put(playerName, entry);
        entry.getView().activateLiveness();

        GameController controller = activeGames.get(playerName);
        if (controller != null) {
            entry.setGameQueue(controller.getGameCommandQueue());
            enqueue(controller.getQueue(), new PlayerReconnectedCommand(playerName, entry.getView()));
        } else {
            entry.getView().sendLobbyList(currentLobbyListDto());
        }
    }

    // ─── LobbyCommandVisitor: menu commands ───────────────────────────────

    /** Sends the requesting player the current list of open lobbies. */
    @Override
    public void visit(ListLobbiesCommand cmd) {
        VirtualView view = getView(cmd.playerName());
        if (view != null) view.sendLobbyList(currentLobbyListDto());
    }

    /**
     * Handles a create-lobby request. Sends the player an error and returns if they
     * are already in a game, already in a lobby, or asked for a size outside the
     * allowed 2–5 range. Otherwise, it creates the lobby, adds the player, and
     * refreshes the lobby list for browsing players.
     */
    @Override
    public void visit(CreateLobbyCommand cmd) {
        PlayerEntry entry = connectedPlayers.get(cmd.playerName());
        if (entry == null) return;

        if (activeGames.containsKey(entry.getName())) {
            entry.getView().sendError("Already in game");
            return;
        }

        if (playerAlreadyInLobby(cmd.playerName())) {
            entry.getView().sendError("Already in lobby");
            return;
        }

        if (cmd.playersNumber() < 2 || cmd.playersNumber() > 5) {
            entry.getView().sendError("Invalid player number: must be between 2 and 5");
            return;
        }

        Lobby lobby = new Lobby(cmd.playerName() + "'s lobby", cmd.playersNumber());
        lobbies.put(lobby.getId(), lobby);
        lobby.addPlayer(entry);
        broadcastLobbyListToBrowsers();
    }

    /**
     * Handles a join-lobby request. Sends the player an error and returns if they
     * are already in a game or a lobby, or if the target lobby is missing or full.
     * Otherwise, it adds them and, if that fills the lobby, starts the game (see
     * {@link #startIfFull}). The browsers' lobby list is refreshed either way.
     */
    @Override
    public void visit(JoinLobbyCommand cmd) {
        PlayerEntry entry = connectedPlayers.get(cmd.playerName());
        if (entry == null) return;

        if (activeGames.containsKey(entry.getName())) {
            entry.getView().sendError("Already in game");
            return;
        }

        if (playerAlreadyInLobby(cmd.playerName())) {
            entry.getView().sendError("Already in lobby");
            return;
        }

        Lobby lobby = lobbies.get(cmd.lobbyId());
        if (lobby == null || lobby.isFull()) {
            entry.getView().sendError("Lobby not found");
            broadcastLobbyListToBrowsers();
            return;
        }

        lobby.addPlayer(entry);
        startIfFull(lobby);
        broadcastLobbyListToBrowsers();
    }

    /**
     * Handles a leave request, branching on the player's state:
     * <ul>
     *   <li><b>in a finished game</b> ({@code END_OF_GAME}): forwards the command to
     *       the session queue (the controller cleans up the model), removes the
     *       entry from {@link #activeGames}, and — once no connected player remains
     *       in that session — drops any leftover (disconnected) entries and shuts
     *       the controller down, off-loading the blocking join to {@code shutdown-executor};</li>
     *   <li><b>in a lobby</b> (pre-game): handled locally by
     *       {@link #handleLeaveFromLobby} (the model is not touched);</li>
     *   <li><b>in game or browsing</b>: an error is sent back to the player.</li>
     * </ul>
     */
    @Override
    public void visit(LeaveCommand cmd) {
        String playerName = cmd.getPlayerName();
        GameController controller = activeGames.get(playerName);

        if (controller != null && controller.isGameOver()) {
            enqueue(controller.getQueue(), cmd);
            activeGames.remove(playerName);

            // Note: this "any connected player left in the session?" scan reads LobbyManager
            // state (activeGames + connectedPlayers), not GameController state;
            boolean stillHasConnected = activeGames.entrySet().stream()
                    .filter(e -> e.getValue() == controller)
                    .anyMatch(e -> connectedPlayers.containsKey(e.getKey()));
            if (!stillHasConnected) {
                activeGames.values().removeIf(c -> c == controller);
                shutdownExecutor.submit(controller::shutdown);
            }

            VirtualView leavingView = getView(playerName);
            if (leavingView != null) {
                leavingView.sendError("LEFT_GAME: success");
                leavingView.sendLobbyList(currentLobbyListDto());
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
     * Disconnect teardown, run on the lobby-thread. If the player was in a running
     * game it enqueues a {@link PlayerDisconnectedCommand} on that session's queue
     * (the controller handles disconnection/suspension). It then removes the player from
     * {@link #connectedPlayers} and closes their view (the blocking close is
     * off-loaded to {@code shutdown-executor}), and finally removes them from any hosting
     * lobby — dropping the lobby if empty and refreshing the browsers.
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
            shutdownExecutor.submit(view::close);
        }

        handleLeaveFromLobby(playerName);
    }

    // ─── Game start ───────────────────────────────────────────────────────

    /**
     * Starts the game for a lobby that has just filled up: notifies the members,
     * removes the lobby from {@link #lobbies}, creates and starts a
     * {@link GameController}, and maps every player to it in {@link #activeGames}.
     * No-op if the lobby is not full.
     */
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

    /** @return {@code true} if the player is a member of any open lobby. */
    private boolean isPlayerInLobby(String playerName) {
        return lobbies.values().stream()
                .flatMap(l -> l.getPlayers().stream())
                .anyMatch(p -> p.getName().equals(playerName));
    }

    /**
     * Removes a player from their lobby (dropping the lobby if it empties), confirms
     * the leave and resends that player the lobby list, then refreshes browsers.
     * Sends an error if the player is not in any lobby.
     */
    private void handleLeaveFromLobby(String playerName) {
        Lobby lobbyToLeave = lobbies.values().stream()
                .filter(l -> l.containsPlayer(playerName))
                .findFirst()
                .orElse(null);
        VirtualView leavingView = getView(playerName);

        if (lobbyToLeave == null) {
            if (leavingView != null) leavingView.sendError("LEAVE_INVALID: not in lobby");
            return;
        }

        lobbyToLeave.removePlayerByName(playerName);
        if (lobbyToLeave.isEmpty()) {
            lobbies.remove(lobbyToLeave.getId());
        }

        if (leavingView != null) {
            leavingView.sendError("LEFT_LOBBY: success");
            leavingView.sendLobbyList(currentLobbyListDto());
        }

        broadcastLobbyListToBrowsers();
    }

    /** @return DTOs for the lobbies that still have room, in insertion order. */
    private List<LobbyDto> currentLobbyListDto() {
        return lobbies.values().stream()
                .filter(l -> !l.isFull())
                .map(Lobby::toDto)
                .toList();
    }

    /** @return the named player's view, or {@code null} if they are not connected. */
    private VirtualView getView(String playerName) {
        PlayerEntry entry = connectedPlayers.get(playerName);
        return entry != null ? entry.getView() : null;
    }

    /** @return {@code true} if a connected player already holds this name. */
    private boolean nameAlreadyTaken(String name) {
        return connectedPlayers.containsKey(name);
    }

    /** @return {@code true} if the player is already a member of some lobby. */
    private boolean playerAlreadyInLobby(String playerName) {
        return lobbies.values().stream().anyMatch(l -> l.containsPlayer(playerName));
    }

    /**
     * Pushes the current open-lobby list to browsing players only. Call whenever
     * the visible set of lobbies changes.
     */
    private void broadcastLobbyListToBrowsers() {
        List<LobbyDto> list = currentLobbyListDto();
        browsingPlayers().forEach(e -> e.getView().sendLobbyList(list));
    }

    /** @return connected players that are browsing: in no lobby and no active game. */
    private List<PlayerEntry> browsingPlayers() {
        return connectedPlayers.values().stream()
                .filter(e -> !activeGames.containsKey(e.getName()))
                .filter(e -> lobbies.values().stream().noneMatch(l -> l.containsPlayer(e.getName())))
                .toList();
    }

    /**
     * Puts a command on a session queue, restoring the interrupt flag instead of
     * propagating {@link InterruptedException}. Used for the lifecycle commands the
     * LobbyManager forwards to a {@link GameController}.
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
     * Orderly shutdown, invoked from a {@code ServerMain} JVM hook (a different
     * thread). Stops and joins the lobby-thread first so this thread becomes the
     * sole accessor of the maps, then shuts every active {@link GameController}
     * down, closes all outbound views, clears the maps and stops the
     * {@code shutdown-executor}. The instance is unusable afterwards.
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
        shutdownExecutor.shutdownNow();
    }
}
