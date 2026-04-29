package network.server.core;

import controller.GameController;
import model.GameModel;
import shared.command.GameCommand;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

public class Game {

    private final List<PlayerEntry> players;
    private final List<VirtualView> views;
    private final BlockingQueue<GameCommand> commandQueue;
    private final GameModel model;
    private final GameController controller;
    private volatile boolean gameOver = false;
    private Thread gameThread;

    public Game(List<PlayerEntry> players, BlockingQueue<GameCommand> commandQueue) {
        this.players = List.copyOf(players);
        this.views = players.stream().map(PlayerEntry::getView).toList();
        this.commandQueue = commandQueue;
        this.model = new GameModel(List.copyOf(views));
        this.controller = new GameController(model);
        controller.startGame(players.stream().map(PlayerEntry::getName).toList());
    }

    public void start() {
        players.forEach(p -> p.setGameQueue(commandQueue));
        QueueDrainerThread qdt = new QueueDrainerThread(commandQueue, new ControllerCommandExecutor(controller),
                (playerName, errorMsg) -> findView(playerName).ifPresent(v -> v.sendError(errorMsg)));
        gameThread = new Thread(qdt, "game-thread");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    public synchronized void onPlayerDisconnected(String playerName) {
        if (gameOver) return;
        views.forEach(v -> v.sendError("Player_disconnected:" + playerName));
        findView(playerName).ifPresent(VirtualView::close);
        this.model.getPlayerByName(playerName).setDisconnected();
    }

    public synchronized void onPlayerReconnected(String playerName, PlayerEntry entry) {
        if (gameOver) return;
        views.forEach(v -> v.sendError("player_reconnected:" + playerName));

        // TODO: rivedere la riconnessione
        this.players.add(entry);
        entry.setGameQueue(commandQueue);
        this.model.getPlayerByName(playerName).setConnected();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private Optional<VirtualView> findView(String playerName) {
        return views.stream()
                .filter(v -> v.getPlayerName().equals(playerName))
                .findFirst();
    }

    private void stopGameThread() {
        if (gameThread != null) gameThread.interrupt();
    }
}
