package controller;

import model.GameModel;
import model.enums.GamePhase;
import model.phaseHandlers.EndOfGamePhase;
import model.phaseHandlers.GamePhaseHandler;
import model.player.Player;
import network.server.core.PlayerEntry;
import network.server.core.VirtualView;
import shared.command.ClientCommand;
import shared.command.CommandDispatcher;
import shared.command.GameCommand;
import shared.command.HeartbeatCommand;
import shared.command.LeaveCommand;
import shared.command.LobbyCommand;
import shared.command.LobbyCommandVisitor;
import shared.command.PlayerDisconnectedCommand;
import shared.command.PlayerReconnectedCommand;
import shared.command.SuspensionTimeoutCommand;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Motore della sessione di gioco. Possiede la coda dei comandi e il game
 * thread: questo thread è l'unico che muta il {@link GameModel}, per
 * costruzione.
 *
 * <h2>Dispatch</h2>
 * Il {@code run()} estrae un {@link ClientCommand} per volta e gli fa
 * {@code cmd.accept(this)}. Il controller implementa due ruoli sul visitor
 * pattern:
 * <ul>
 *   <li>{@link CommandDispatcher}: smista in base al sotto-tipo
 *       ({@code GameCommand} → phase handler;
 *       {@code LobbyCommand} → visitor interno);</li>
 *   <li>{@link LobbyCommandVisitor}: gestisce {@link LeaveCommand},
 *       {@link PlayerDisconnectedCommand}, {@link PlayerReconnectedCommand}
 *       e {@link SuspensionTimeoutCommand}; gli altri sotto-tipi di
 *       {@code LobbyCommand} hanno default no-op nel visitor e non
 *       interessano al controller (li gestisce {@code LobbyManager} prima
 *       dell'impilamento).</li>
 * </ul>
 *
 * <h2>Sospensione e resilienza alle disconnessioni</h2>
 * Quando rimane un solo player connesso la partita entra in stato sospeso
 * (flag {@link #suspended}, scrutinato in {@link #onGameCommand}). Un timer
 * di {@value #SUSPENSION_TIMEOUT_SECONDS} secondi viene schedulato sul
 * {@link #suspensionScheduler}; il task NON tocca il model direttamente,
 * impila invece un {@link SuspensionTimeoutCommand} sulla coda così che il
 * fine partita avvenga sul game thread come ogni altro evento. Se prima del
 * timeout almeno un altro player si riconnette il future viene cancellato e
 * il flag {@code suspended} torna false.
 *
 * <h2>Costruttori</h2>
 * <ul>
 *   <li>{@link #GameController(List)} è il costruttore di produzione: riceve
 *       i {@link PlayerEntry} di una lobby completa, costruisce il
 *       {@link GameModel}, inizializza la partita e crea il game thread
 *       (non avviato). {@link #start()} fa il wiring delle command queue sui
 *       player ed avvia il thread; {@link #shutdown()} lo ferma.</li>
 *   <li>{@link #GameController(GameModel)} è un costruttore minimale per i
 *       test: assegna il model passato, non crea thread; chi lo usa deve
 *       invocare {@code startGame()} e {@code handleCommand()} sincroni.</li>
 * </ul>
 */
public final class GameController implements Runnable, CommandDispatcher, LobbyCommandVisitor {

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
    private static final long SUSPENSION_TIMEOUT_SECONDS = 30;

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
    @SuppressWarnings("unchecked")
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
                cmd.accept(this); // CommandDispatcher.onXxxCommand(cmd)
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

    // ─── CommandDispatcher ───────────────────────────────────────────────

    @Override
    public void onGameCommand(GameCommand cmd) {
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
    public void onLobbyCommand(LobbyCommand cmd) throws Exception {
        // Cast esplicito per disambiguare tra LobbyCommand.accept(CommandDispatcher)
        // e LobbyCommand.accept(LobbyCommandVisitor): qui vogliamo il secondo.
        cmd.accept((LobbyCommandVisitor) this);
    }

    @Override
    public void onHeartbeatCommand(HeartbeatCommand cmd) {
        // Mai instradato sulla coda del game: gli endpoint lo passano
        // direttamente al LobbyManager. Eventuali heartbeat finiti qui per
        // errore vengono ignorati.
    }

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

    // ─── LobbyCommandVisitor — solo i tipi di interesse del controller ───

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

        // ─── Sospensione: scatta quando rimane un solo connesso ────────
        long connected = countConnected();
        if (connected == 1 && !suspended) {
            suspended = true;
            for (VirtualView v : model.getViews()) {
                v.sendError("GAME_SUSPENDED: in attesa di una riconnessione, timer " + SUSPENSION_TIMEOUT_SECONDS + "s");
            }
            suspensionTimeoutFuture = suspensionScheduler.schedule( () -> enqueueSelf(new SuspensionTimeoutCommand()),
                    SUSPENSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } else if (connected == 0) {
            // Anche l'ultimo player è uscito durante la sospensione: termina
            // la partita calcolando i punteggi sullo stato corrente. La
            // session resta in activeGames (lazy cleanup), verrà rimossa
            // quando un eventuale player riconnesso farà LeaveCommand.
            cancelSuspensionTimer();
            suspended = false;
            model.setPhase(new EndOfGamePhase(model));
            model.setGameOver(); // TODO: perche non lo fa EndOfGamePhase?
            model.notifyChange();
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
        model.notifyChange();
    }

    @Override
    public void visit(SuspensionTimeoutCommand cmd) {
        // Guard: il reconnect potrebbe aver cancellato la sospensione
        // mentre il task era già in coda, oppure aver vinto la corsa col
        // suspensionScheduler. In quel caso non c'è nulla da fare.
        if (!suspended) return;
        suspended = false;

        String winner = model.getPlayers().stream()
                .filter(Player::isConnected)
                .map(Player::getName)
                .findFirst()
                .orElse(null);

        if (winner == null) {
            // Edge: nessuno è più connesso (race con disconnect dell'ultimo).
            model.setPhase(new EndOfGamePhase(model));
        } else {
            model.setWinners(List.of(winner));
        }
        model.setGameOver();
        model.notifyChange();
    }

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
