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
        QueueDrainerThread qdt = new QueueDrainerThread(commandQueue, controller,
                // TODO: leva consumer
                (playerName, errorMsg) -> findView(playerName).ifPresent(v -> v.sendError(errorMsg)));
        gameThread = new Thread(qdt, "game-thread");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    public synchronized void onPlayerDisconnect(String playerName) {
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
        this.model.swapView(entry.getName(), entry.getView());
        this.model.notifyChange();
    }

    /**
     * Called when a player explicitly leaves the game (only valid in END_OF_GAME phase).
     * Removes the player from the game and notifies remaining players.
     * If all players have left, the game can be cleaned up by the server.
     */
    public synchronized void onPlayerLeft(String playerName) {
        if (!isGameOver()) return;

        try {
            // Remove from views
            Optional<VirtualView> oldView = findView(playerName);
            oldView.ifPresent(this.views::remove);

            // Remove from players
            findPlayerEntry(playerName).ifPresent(this.players::remove);

            // Mark player as disconnected in model
            this.model.getPlayerByName(playerName).setDisconnected();

            // Notify remaining players
            views.forEach(v -> v.sendError("player_left:" + playerName));
        } catch (Exception e) {
            views.forEach(v -> v.sendError("ERROR_leaving_lobby" + e.getMessage()));
        }
    }

    public boolean isGameOver() {
        return this.model.isGameOver();
    }

    /**
     * Returns a copy of the list of active players in this game.
     * Used to check if the game should be cleaned up.
     */
    public List<PlayerEntry> getActivePlayers() {
        return new ArrayList<>(players);
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
