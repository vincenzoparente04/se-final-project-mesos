package controller;

import model.GameModel;
import model.enums.GamePhase;
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
import model.phaseHandlers.GamePhaseHandler;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
 *       {@link PlayerDisconnectedCommand} e {@link PlayerReconnectedCommand};
 *       gli altri sotto-tipi di {@code LobbyCommand} hanno default no-op nel
 *       visitor e non interessano al controller (li gestisce
 *       {@code LobbyManager} prima dell'impilamento).</li>
 * </ul>
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
 *
 * <h2>API pubblica sincrona</h2>
 * {@link #handleCommand(GameCommand)} resta pubblico e sincrono come
 * nell'impianto originale: validate del turno e delega a
 * {@code model.handleCommand}, propagando le eccezioni. È invocato sia dal
 * dispatch interno che dai test.
 */
public final class GameController implements Runnable, CommandDispatcher, LobbyCommandVisitor {

    private final GameModel model;
    private final BlockingQueue<ClientCommand> queue = new LinkedBlockingQueue<>();
    private final List<PlayerEntry> players;
    private final Thread gameThread;
    private volatile boolean running = true;

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

    // TODO si puo levare ma vedi se serve per i test
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
     * Stop the game thread cleanly. Sets the running flag, interrupts the
     * thread to unblock {@code take()}, and joins for up to 2 seconds. Only
     * valid for instances built via {@link #GameController(List)}.
     */
    public void shutdown() {
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

    // TODO: vedi se si può fare un unico visitor che fa override su GameCommand e sui 3 comandi di rete direttamente

    // ─── CommandDispatcher ───────────────────────────────────────────────

    @Override
    public void onGameCommand(GameCommand cmd) {
        Player current = model.getCurrentPlayer();
        if (current != null && !current.isConnected()) {
            // Comando arrivato in coda prima di un disconnect che ha già
            // marcato il player offline: ignora silenziosamente.
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
        cmd.accept((LobbyCommandVisitor) this); // TODO: non mi piace questo cast, magari istanziamolo e facciamo this.lobbyComandVisitor
    }

    @Override
    public void onHeartbeatCommand(HeartbeatCommand cmd) {
        // Mai instradato sulla coda del game: gli endpoint lo passano
        // direttamente al LobbyManager. Eventuali heartbeat finiti qui per
        // errore vengono ignorati.
    } // TODO: vediamo se si possono pulire i visitor e togliere questi override inutili.

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

    // TODO: mettere un filtro che se è rimasto un player solo fa scattare un timer e setta un bool isSuspended();
    //  ad ogni comando spilato si controlla questo bool, se la partita è sospesa non fa nulla, se il comando è un
    //  PlayerReconnectedCommand il boolean è settato false e posso continuare a giocare. Se il timeout scade
    //  bisogna fare setWinners, setGameOver e notifyChange

    // TODO: se anche l'ultimo player restante crasha la partita deve essere eliminata in qualche modo
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
        if (phaseHandler != null && phaseHandler.getCurrentPlayer() != null && phaseHandler.getCurrentPlayer().getName().equals(cmd.getPlayerName())) {
            phaseHandler.skipCurrentPlayerTurn();
        }
    }

    // TODO: dovrà settare il bool suspended a false
    @Override
    public void visit(PlayerReconnectedCommand cmd) {
        try {
            model.swapView(cmd.getPlayerName(), cmd.newView());
            model.getPlayerByName(cmd.getPlayerName()).setConnected();
            model.notifyChange();
        } catch (IllegalArgumentException ignored) {
            // player non in questa session — nulla da fare
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private Optional<VirtualView> findView(String playerName) {
        return model.getViews().stream()
                .filter(v -> v.getPlayerName().equals(playerName))
                .findFirst();
    }
}
