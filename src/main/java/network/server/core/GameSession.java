package network.server.core;

import controller.GameController;
import model.GameModel;
import shared.command.ClientCommand;
import shared.command.GameCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

/**
 * Network-side adapter per una partita in corso. Thin wrapper sopra il
 * {@link GameController}: lo costruisce, gli passa i player iniziali, parte
 * il game thread, lo ferma in shutdown. Espone la coda al
 * {@code LobbyManager} (che impila i lifecycle command) e
 * {@code isGameOver()} (lettura del flag del model).
 *
 * <p>Non ha alcun metodo {@code submitXxx}: i client (endpoint di rete /
 * LobbyManager) interagiscono direttamente con la coda ritornata da
 * {@link #getCommandQueue()}, senza passare per altri livelli di indirezione.
 */
public final class GameSession {

    // TODO forse questa classe si può levare e collassare nel controller

    private final GameModel model;
    private final GameController controller;
    private final Thread gameThread;

    public GameSession(List<PlayerEntry> players) {
        List<VirtualView> views = players.stream()
                .map(PlayerEntry::getView)
                .collect(Collectors.toCollection(ArrayList::new));
        this.model = new GameModel(views);
        this.controller = new GameController(model);
        controller.startGame(players.stream().map(PlayerEntry::getName).toList());
        this.gameThread = new Thread(controller, "game-thread");
        this.gameThread.setDaemon(true);
    }

    // TODO questo sotto può farlo il controller ad inizio partita
    /** Wire each player's command queue to the controller's queue and start the game thread. */
    public void start(List<PlayerEntry> players) {
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
    public BlockingQueue<ClientCommand> getCommandQueue() {
        return controller.getQueue();
    }

    /**
     * Same instance as {@link #getCommandQueue()}, exposed with the
     * narrower {@code BlockingQueue<GameCommand>} type. This is the view
     * given to {@link PlayerEntry#setGameQueue(BlockingQueue)}: gli
     * endpoint di rete metteranno esclusivamente {@code GameCommand}
     * (sottotipi di {@code ClientCommand}), quindi il cast è sicuro per
     * costruzione.
     */
    @SuppressWarnings("unchecked")
    public BlockingQueue<GameCommand> getGameCommandQueue() {
        return (BlockingQueue<GameCommand>) (BlockingQueue<?>) controller.getQueue();
    }

    public boolean isGameOver() {
        return controller.isGameOver();
    }

    /**
     * Order the game thread to exit and wait briefly. The {@code requestShutdown}
     * sets the running flag; the {@code interrupt} unblocks the {@code take()}
     * the thread may be sitting on; the {@code join} gives it up to 2 seconds
     * to leave cleanly.
     */
    public void shutdown() {
        controller.requestShutdown();
        gameThread.interrupt();
        try {
            gameThread.join(2000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
