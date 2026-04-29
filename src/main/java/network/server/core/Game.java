package network.server.core;

import controller.GameController;
import model.GameModel;
import model.phaseHandlers.GamePhaseHandler;
import shared.command.GameCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class Game {

    private final List<PlayerEntry> players;
    private final List<VirtualView> views;
    private final BlockingQueue<GameCommand> commandQueue;
    private final GameModel model;
    private final GameController controller;
    private volatile boolean gameOver = false;
    private Thread gameThread;

    public Game(List<PlayerEntry> players, BlockingQueue<GameCommand> commandQueue) {
        this.players = new ArrayList<>(players);
        this.views = players.stream().map(PlayerEntry::getView).collect(Collectors.toCollection(ArrayList::new)); // Idem
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
        findPlayerEntry(playerName).ifPresent(this.players::remove);

        this.model.getPlayerByName(playerName).setDisconnected();
        //findView(playerName).ifPresent(this.model::removeView);
        GamePhaseHandler currentPhase = model.getPhaseHandler();
        if (currentPhase.getCurrentPlayer() != null && currentPhase.getCurrentPlayer().getName().equals(playerName)) {
            currentPhase.skipCurrentPlayerTurn(); // forces turn advance
        }
    }

    public synchronized void onPlayerReconnected(String playerName, PlayerEntry entry) {
        if (gameOver) return;
        views.forEach(v -> v.sendError("player_reconnected:" + playerName));

        this.players.add(entry);
        this.views.add(entry.getView());
        entry.setGameQueue(commandQueue);
        this.model.getPlayerByName(playerName).setConnected();
        this.model.addView(entry.getView());
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private Optional<VirtualView> findView(String playerName) {
        return views.stream()
                .filter(v -> v.getPlayerName().equals(playerName))
                .findFirst();
    }

    private Optional<PlayerEntry> findPlayerEntry(String playerName) {
        return players.stream().filter(p -> p.getName().equals(playerName)).findFirst();
    }

    private void stopGameThread() {
        if (gameThread != null) gameThread.interrupt();
    }
}
