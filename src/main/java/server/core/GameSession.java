package server.core;

/*
 * Threading model:
 * - A single thread (GameThread) accesses GameController from the command side.
 * - DTO broadcasts are asynchronous via broadcastExecutor.
 * - All VirtualView.send* implementations must be non-blocking or executed
 *   off-thread in their own implementations.
 */

import controller.GameController;
import model.GameModel;
import shared.command.GameCommand;
import shared.dto.GameStateDto;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Infrastructure container for one game instance.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Accepts a {@link List} of {@link PlayerEntry} objects — one per
 *       player, regardless of transport protocol (socket or RMI).</li>
 *   <li>On {@link #start()}: activates each player entry, subscribes
 *       to the controller's DTO stream and broadcasts snapshots to all views
 *       asynchronously via a thread pool, starts the game, and launches the
 *       {@link GameThread} that processes commands sequentially.</li>
 *   <li>On player disconnect: notifies the remaining players and shuts down
 *       the game thread.</li>
 * </ul>
 * No static state — multiple instances can run simultaneously.
 */
public class GameSession {

    private final List<PlayerEntry> players;
    private final List<VirtualView> views;
    private final BlockingQueue<GameCommand> commandQueue;
    private final GameController controller;
    private final GameModel model;
    private final ExecutorService broadcastExecutor;
    private volatile boolean gameOver = false;
    private Thread gameThread;

    public GameSession(List<PlayerEntry> players,
                       BlockingQueue<GameCommand> commandQueue) {
        this.players = List.copyOf(players);
        this.views = players.stream().map(PlayerEntry::getView).toList();
        this.commandQueue = commandQueue;
        this.model = new GameModel();
        this.controller = new GameController(model);
        this.broadcastExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "broadcast");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts the game:
     * <ol>
     *   <li>Activates each player entry (starts socket reading threads or
     *       installs RMI disconnect handlers) via {@link PlayerEntry#activate}.</li>
     *   <li>Subscribes to the DTO stream so every model change is broadcast
     *       asynchronously to all clients.</li>
     *   <li>Starts the game model and the {@link GameThread}.</li>
     * </ol>
     */
    public void start() {
        List<String> playerNames = players.stream().map(PlayerEntry::getName).toList();

        players.forEach(p -> p.activate(commandQueue, this::onPlayerDisconnected));

        for(PlayerEntry player: players) {
            model.addViewListener(player.getView());
        }

        /*
        controller.addDtoListener(dto -> {
            boolean isFinal = dto.winners != null && !dto.winners.isEmpty();
            if (isFinal) {
                gameOver = true;
                stopGameThread();
            }
            broadcastAsync(dto);
            if (isFinal) {
                // schedule shutdown AFTER in-flight broadcasts
                new Thread(this::shutdownBroadcastExecutor, "broadcast-shutdown").start();
            }
        });*/

        controller.startGame(playerNames);

        GameThread gt = new GameThread(
                commandQueue,
                new ControllerCommandExecutor(controller),
                (playerName, errorMsg) ->
                        findView(playerName).ifPresent(v -> v.sendError(errorMsg)));
        gameThread = new Thread(gt, "game-thread");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    /**
     * Called when a player disconnects unexpectedly.
     * If the game has ended normally this is a no-op (clean shutdown).
     * Otherwise all remaining clients receive an error and the session
     * is torn down.
     */
    public synchronized void onPlayerDisconnected(String playerName) {
        if (gameOver) return;
        views.forEach(v -> v.sendError("player_disconnected:" + playerName));
        views.forEach(VirtualView::close);
        stopGameThread();
        shutdownBroadcastExecutor();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private void broadcastAsync(GameStateDto dto) {
        views.forEach(v -> {
            try {
                broadcastExecutor.submit(() -> v.sendState(dto));
            } catch (java.util.concurrent.RejectedExecutionException ignored) {
                // executor was shut down after a disconnect — broadcast dropped intentionally
            }
        });
    }

    private Optional<VirtualView> findView(String playerName) {
        return views.stream()
                .filter(v -> v.getPlayerName().equals(playerName))
                .findFirst();
    }

    private void stopGameThread() {
        if (gameThread != null) gameThread.interrupt();
    }

    private void shutdownBroadcastExecutor() {
        broadcastExecutor.shutdown();
        try {
            if (!broadcastExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                broadcastExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            broadcastExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
