package network.server.core;

import controller.GameController;
import model.GameModel;
import model.phaseHandlers.GamePhaseHandler;
import shared.command.GameCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Game {

    private final List<PlayerEntry> players;
    private final List<VirtualView> views;
    private final BlockingQueue<GameCommand> commandQueue;
    private final GameModel model;
    private final GameController controller;
    private volatile boolean gameOver = false;
    private Thread gameThread;

    public Game(List<PlayerEntry> players) {
        this.players = new ArrayList<>(players);
        this.views = players.stream()
                .map(PlayerEntry::getView)
                .collect(Collectors.toCollection(ArrayList::new));
        this.commandQueue = new LinkedBlockingQueue<>();
        this.model = new GameModel(this.views);
        this.controller = new GameController(model);
        controller.startGame(players.stream().map(PlayerEntry::getName).toList());
    }

    public void start() {
        players.forEach(p -> p.setGameQueue(commandQueue));
        QueueDrainerThread qdt = new QueueDrainerThread(commandQueue, new ControllerCommandExecutor(controller),
                // TODO: leva consumer
                (playerName, errorMsg) -> findView(playerName).ifPresent(v -> v.sendError(errorMsg)));
        gameThread = new Thread(qdt, "game-thread");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    public synchronized void onPlayerDisconnect(String playerName) {
        // TODO: forse può delegare al model la notifica delle view, in questo modo Game non conosce più le view, pensiamoci
        if (gameOver) return;
        views.forEach(v -> v.sendError("Player_disconnected:" + playerName));

        Optional<VirtualView> disconnectedView = findView(playerName);
        if (disconnectedView.isPresent()) {
            disconnectedView.get().close();
            this.views.remove(disconnectedView.get());
        }
        findPlayerEntry(playerName).ifPresent(this.players::remove);

        this.model.getPlayerByName(playerName).setDisconnected();

        GamePhaseHandler currentPhase = model.getPhaseHandler();
        if (currentPhase.getCurrentPlayer() != null && currentPhase.getCurrentPlayer().getName().equals(playerName)) {
            currentPhase.skipCurrentPlayerTurn(); // forces turn advance
        }
    }

    public synchronized void onPlayerReconnected(String playerName, PlayerEntry entry) {
        if (gameOver) return;

        this.players.add(entry);
        this.views.add(entry.getView());
        this.players.getLast().setGameQueue(commandQueue);
        this.model.getPlayerByName(playerName).setConnected();
        this.model.addView(entry.getView());
        this.model.notifyChange();
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
