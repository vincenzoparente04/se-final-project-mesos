package controller;

import model.GameModel;
import model.enums.GamePhase;
import model.phaseHandlers.EndOfGamePhase;
import model.phaseHandlers.GamePhaseHandler;
import model.player.Player;
import network.server.core.PlayerEntry;
import network.server.core.VirtualView;
import shared.command.ClientCommand;
import shared.command.ClientCommandVisitor;
import shared.command.gameCommand.GameCommand;
import shared.command.lobbyCommand.HeartbeatCommand;
import shared.command.lobbyCommand.LeaveCommand;
import shared.command.lobbyCommand.LobbyCommand;
import shared.command.lobbyCommand.LobbyCommandVisitor;
import shared.command.lobbyCommand.PlayerDisconnectedCommand;
import shared.command.lobbyCommand.PlayerReconnectedCommand;
import shared.command.lobbyCommand.SuspensionTimeoutCommand;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Engine for a game session. Holds the command queue and the game thread:
 * this thread is the only one that mutates the {@link GameModel}, by
 * construction.
 *
 * <h2>Dispatch</h2>
 * The {@code run()} method takes one {@link ClientCommand} at a time and
 * calls {@code cmd.accept(this)}. The controller implements
 * {@link ClientCommandVisitor} to dispatch based on the command family
 * ({@link GameCommand} → {@link #handleCommand}, {@link LobbyCommand} →
 * {@link #lobbyCommandVisitor}, {@link HeartbeatCommand} → no-op). The four
 * lifecycle commands of interest ({@link LeaveCommand},
 * {@link PlayerDisconnectedCommand}, {@link PlayerReconnectedCommand},
 * {@link SuspensionTimeoutCommand}) are handled by the anonymous
 * {@link #lobbyCommandVisitor}.
 *
 * <h2>Suspension and disconnection resilience</h2>
 * When only one player remains connected the match enters a suspended state
 * (flag {@link #suspended}, inspected in {@link #visit(GameCommand)}). A
 * timer of {@value #SUSPENSION_TIMEOUT_SECONDS} seconds is scheduled on the
 * {@link #suspensionScheduler}; the task does NOT touch the model directly
 * but instead enqueues a {@link SuspensionTimeoutCommand} so that the
 * end-of-game happens on the game thread like any other event. If at least
 * one other player reconnects before the timeout the future is cancelled
 * and the {@code suspended} flag is reset to false.
 *
 * <h2>Constructors</h2>
 * <ul>
 *   <li>{@link #GameController(List)} is the production constructor: it
 *       receives the {@link PlayerEntry} objects from a full lobby, builds
 *       the {@link GameModel}, initializes the match and creates the game
 *       thread (not started). {@link #start()} wires the players' command
 *       queues to the controller and starts the thread; {@link #shutdown()}
 *       stops it.</li>
 *   <li>{@link #GameController(GameModel)} is a minimal constructor used for
 *       tests: it assigns the provided model and does not create a thread;
 *       callers must invoke {@code startGame()} and invoke
 *       {@code handleCommand()} synchronously.</li>
 * </ul>
 */
public final class GameController implements Runnable, ClientCommandVisitor {

    private final GameModel model;
    private final BlockingQueue<ClientCommand> queue = new LinkedBlockingQueue<>();
    private final List<PlayerEntry> players;
    private final Thread gameThread;
    private volatile boolean running = true;
    private volatile boolean suspended = false;

    private final ScheduledExecutorService suspensionScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "suspension-timer");
                t.setDaemon(true);
                return t;
            });
    private ScheduledFuture<?> suspensionTimeoutFuture = null;
    private static final long SUSPENSION_TIMEOUT_SECONDS = 60;

    /**
     * Test-only constructor: receives a pre-built {@link GameModel} and does
     * not own a game thread. {@link #start()} and {@link #shutdown()} are
     * undefined for instances built this way.
     */
    public GameController(GameModel model) {
        this.model = model;
        this.players = List.of();
        this.gameThread = null;
    }

    /**
     * Production constructor. Builds the {@link GameModel} from the players'
     * views, kicks off the game (creates the phase handler) and prepares the
     * game thread. The thread is started by {@link #start()}.
     */
    public GameController(List<PlayerEntry> players) {
        this.players = List.copyOf(players);
        List<VirtualView> views = players.stream().map(PlayerEntry::getView).toList();
        this.model = new GameModel(views);
        model.startGame(players.stream().map(PlayerEntry::getName).toList());
        this.gameThread = new Thread(this, "game-thread");
        this.gameThread.setDaemon(true);
    }

    public void startGame(List<String> playerNames) {
        model.startGame(playerNames);
    }

    /**
     * Wires every player's command queue to this controller's queue and
     * starts the game thread. Only valid for instances built via
     * {@link #GameController(List)}.
     */
    public void start() {
        BlockingQueue<GameCommand> q = getGameCommandQueue();
        players.forEach(p -> p.setGameQueue(q));
        gameThread.start();
    }

    /**
     * The controller's internal queue, typed on {@link ClientCommand}. The
     * {@code LobbyManager} uses this view to impilare i lifecycle command
     * ({@code PlayerDisconnectedCommand}, {@code PlayerReconnectedCommand},
     * {@code LeaveCommand} during END_OF_GAME).
     */
    public BlockingQueue<ClientCommand> getQueue() {
        return queue;
    }

    /**
     * Same instance as {@link #getQueue()}, exposed with the narrower
     * {@code BlockingQueue<GameCommand>} type. This is the view given to
     * {@link PlayerEntry#setGameQueue(BlockingQueue)}: gli endpoint di rete
     * metteranno esclusivamente {@code GameCommand} (sottotipi di
     * {@code ClientCommand}), quindi il cast è sicuro per costruzione.
     */
    public BlockingQueue<GameCommand> getGameCommandQueue() {
        return (BlockingQueue<GameCommand>) (BlockingQueue<?>) queue;
    }

    public boolean isGameOver() {
        return model.isGameOver();
    }

    /**
     * Stop the game thread cleanly. Cancels any pending suspension timer,
     * shuts down the scheduler, sets the running flag, interrupts the
     * thread to unblock {@code take()}, and joins for up to 2 seconds. Only
     * valid for instances built via {@link #GameController(List)}.
     */
    public void shutdown() {
        cancelSuspensionTimer();
        suspensionScheduler.shutdownNow();
        running = false;
        gameThread.interrupt();
        try {
            gameThread.join(2000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ─── Game thread loop ────────────────────────────────────────────────

    @Override
    public void run() {
        while (running) {
            try {
                ClientCommand cmd = queue.take();
                cmd.accept(this); // ClientCommandVisitor.visit(...)
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // A handler-level exception must never kill the game thread.
                // Per-handler code already catches expected exceptions and
                // routes the error to the responsible player's view.
                System.err.println("[game-thread] unexpected error: " + e);
            }
        }
    }

    // ─── ClientCommandVisitor: famiglia → handler giusto ─────────────────

    @Override
    public void visit(GameCommand cmd) {
        if (suspended) {
            findView(cmd.getPlayerName()).ifPresent(v ->
                    v.sendError("GAME_SUSPENDED: La partita è sospesa, in attesa di riconnessioni.")
            );
            return;
        }

        Player current = model.getCurrentPlayer();
        if (current != null && !current.isConnected()) {
            return;
        }
        try {
            handleCommand(cmd);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            findView(cmd.getPlayerName()).ifPresent(v -> v.sendError(msg));
        }
    }

    @Override
    public void visit(LobbyCommand cmd) throws Exception {
        cmd.accept(lobbyCommandVisitor);
    }

    @Override
    public void visit(HeartbeatCommand cmd) {}

    // Handle Command delegating to the model –––––––––––––––––––––––––––––
    /**
     * Synchronous in-game command execution. Identica firma e semantica del
     * codice originale: valida che sia il turno del player e delega al model.
     * Usata sia dal dispatch interno della coda sia dai test.
     */
    public synchronized void handleCommand(GameCommand cmd) throws Exception {
        Player current = model.getCurrentPlayer();
        if (model.getCurrentPhase() != GamePhase.COLOR_CHOOSING_PHASE) {
            if (current == null || !current.getName().equals(cmd.getPlayerName())) {
                throw new IllegalStateException("It is not " + cmd.getPlayerName() + "'s turn.");
            }
        }
        model.handleCommand(cmd);
    }

    // ─── LobbyCommandVisitor anonimo: solo i 4 lifecycle che ci interessano

    private final LobbyCommandVisitor lobbyCommandVisitor = new LobbyCommandVisitor() {
        @Override
        public void visit(LeaveCommand cmd) {
            if (!model.isGameOver()) return;
            try {
                model.getPlayerByName(cmd.getPlayerName()).setDisconnected();
            } catch (IllegalArgumentException ignored) {
                return;
            }
            model.removeView(cmd.getPlayerName());
            for (VirtualView v : model.getViews()) {
                v.sendError("player_left:" + cmd.getPlayerName());
            }
        }

        @Override
        public void visit(PlayerDisconnectedCommand cmd) {
            Player p;
            try {
                p = model.getPlayerByName(cmd.getPlayerName());
            } catch (IllegalArgumentException ignored) {
                return;
            }
            p.setDisconnected();

            for (VirtualView v : model.getViews()) {
                if (!v.getPlayerName().equals(cmd.getPlayerName())) {
                    v.sendError("Player_disconnected:" + cmd.getPlayerName());
                }
            }
            model.removeView(cmd.getPlayerName());

            GamePhaseHandler phaseHandler = model.getPhaseHandler();
            if (phaseHandler != null && phaseHandler.getCurrentPlayer() != null
                    && phaseHandler.getCurrentPlayer().getName().equals(cmd.getPlayerName())) {
                phaseHandler.skipCurrentPlayerTurn();
            }

            // ─── Suspension: when only a player remains connected ────────
            long connected = countConnected();
            if (connected == 1 && !suspended) {
                suspended = true;
                for (VirtualView v : model.getViews()) {
                    v.sendError("GAME_SUSPENDED: in attesa di una riconnessione, timer " + SUSPENSION_TIMEOUT_SECONDS + "s");
                }
                suspensionTimeoutFuture = suspensionScheduler.schedule(
                        () -> enqueueSelf(new SuspensionTimeoutCommand()),
                        SUSPENSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } else if (connected == 0) {
                // Even the last player dropped during suspension: end the match by
                // forfeit. The session stays in activeGames (lazy cleanup) until a
                // reconnecting player issues a LeaveCommand. EndOfGamePhase.onEnter
                // broadcasts the game-over.
                cancelSuspensionTimer();
                suspended = false;
                model.setGameOver();
                model.setPhase(new EndOfGamePhase(model, null, true));
            }
        }

        @Override
        public void visit(PlayerReconnectedCommand cmd) {
            try {
                model.swapView(cmd.getPlayerName(), cmd.newView());
                model.getPlayerByName(cmd.getPlayerName()).setConnected();
            } catch (IllegalArgumentException ignored) {
                // player non in questa session — nulla da fare
                return;
            }

            if (suspended && countConnected() >= 2) {
                cancelSuspensionTimer();
                suspended = false;
                for (VirtualView v : model.getViews()) {
                    v.sendError("GAME_RESUMED");
                }
            }
            // State first (re-populates the client's LocalGameState, including the
            // players the GUI winner screen reads), then re-send the end-game payload
            // so a player reconnecting to a finished match rebuilds the final screen.
            model.notifyChange();
            if (model.isGameOver()) {
                model.notifyEndGame();
            }
        }

        @Override
        public void visit(SuspensionTimeoutCommand cmd) {
            // Guard: il reconnect potrebbe aver cancellato la sospensione
            // mentre il task era già in coda, oppure aver vinto la corsa col
            // suspensionScheduler. In quel caso non c'è nulla da fare.
            if (!suspended) return;
            suspended = false;

            Player winner = model.getPlayers().stream()
                    .filter(Player::isConnected)
                    .findFirst()
                    .orElse(null);

            // Forfeit: the last connected player wins by default (or nobody, if a
            // final disconnect raced us). No end-game scoring breakdown.
            // EndOfGamePhase.onEnter broadcasts the game-over.
            model.setGameOver();
            model.setPhase(new EndOfGamePhase(model, winner == null ? null : List.of(winner), true));
        }
    };

    // ─── Helpers ─────────────────────────────────────────────────────────

    private Optional<VirtualView> findView(String playerName) {
        return model.getViews().stream()
                .filter(v -> v.getPlayerName().equals(playerName))
                .findFirst();
    }

    private long countConnected() {
        return model.getPlayers().stream().filter(Player::isConnected).count();
    }

    private void enqueueSelf(LobbyCommand cmd) {
        try {
            queue.put(cmd);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void cancelSuspensionTimer() {
        if (suspensionTimeoutFuture != null) {
            suspensionTimeoutFuture.cancel(false);
            suspensionTimeoutFuture = null;
        }
    }
}
