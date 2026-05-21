package model;

import model.board.Board;
import model.enums.Era;
import model.enums.GamePhase;
import model.phaseHandlers.ColorChoosingPhase;
import model.phaseHandlers.GamePhaseHandler;
import model.player.Player;
import model.rowsManager.RowsManager;
import network.server.core.VirtualView;
import shared.command.gameCommand.GameCommand;
import shared.dto.GameStateDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Authoritative game state. Owns the players, the board, the row manager and
 * the current phase handler, and is responsible for notifying its registered
 * {@link VirtualView}s when the state changes.
 *
 * <h2>Threading contract</h2>
 * After construction, {@code GameModel} is mutated exclusively by the
 * single <em>game thread</em> owned by {@code GameController}. All mutators
 * documented as {@code @CalledOnGameThreadOnly} below MUST NOT be invoked
 * from network/handler threads. Read-only getters that are accessed across
 * threads (e.g. {@link #isGameOver()}) are made safe by marking the underlying
 * fields {@code volatile}.
 */
public class GameModel {
    private static final int MAX_ROUNDS = 10;

    private Board board;
    private final RowsManager rowsManager = new RowsManager();
    private List<Player> players = new ArrayList<>();
    private volatile int currentRound = 1;
    private List<String> winners = new ArrayList<>();

    private volatile GamePhaseHandler currentPhaseHandler;
    private final List<VirtualView> views;

    public GameModel(List<VirtualView> views) {
        this.views = new ArrayList<>(views);
    }

    /** Convenience constructor for tests that don't need any view registered. */
    public GameModel() {
        this(List.of());
    }


    /** @apiNote @CalledOnGameThreadOnly */
    public void startGame(List<String> playerNames) {
        this.board = new Board(playerNames.size());
        createPlayers(playerNames);
        setPhase(new ColorChoosingPhase(this));
    }

    private void createPlayers(List<String> playerNames) {
        this.players = new ArrayList<>();
        for (String name : playerNames) {
            this.players.add(new Player(name));
        }
    }

    /** @apiNote @CalledOnGameThreadOnly */
    public void setPhase(GamePhaseHandler phase) {
        this.currentPhaseHandler = phase;
        phase.onEnter();
    }

    /**
     * Single dispatch point for all in-game commands. Snapshots the current
     * phase handler once and visits the command on it: the handler decides,
     * via polymorphic dispatch on the command type, whether and how to
     * execute it.
     *
     * @apiNote @CalledOnGameThreadOnly
     */
    public void handleCommand(GameCommand cmd) throws Exception {
        GamePhaseHandler handler = this.currentPhaseHandler;
        cmd.accept(handler);
    }

    /** @apiNote @CalledOnGameThreadOnly */
    public void notifyChange() {
        GameStateDto dto = GameStateDtoBuilder.build(this);
        for (VirtualView v : new ArrayList<>(views)) {
            v.sendState(dto);
        }
    }

    /**
     * Replace the view associated with {@code playerName}. If no view exists
     * for that player the new view is simply appended.
     *
     * @apiNote @CalledOnGameThreadOnly
     */
    public void swapView(String playerName, VirtualView newView) {
        views.removeIf(v -> v.getPlayerName().equals(playerName));
        views.add(newView);
    }

    /**
     * Remove the view associated with {@code playerName}, if present.
     *
     * @apiNote @CalledOnGameThreadOnly
     */
    public void removeView(String playerName) {
        views.removeIf(v -> v.getPlayerName().equals(playerName));
    }

    /**
     * Snapshot of the current registered views. The returned list is a
     * defensive copy; callers may iterate without holding any lock.
     */
    public List<VirtualView> getViews() {
        return List.copyOf(views);
    }

    /** @apiNote @CalledOnGameThreadOnly */
    public void incrementRound() {
        currentRound++;
    }

    public boolean isGameOver() {
        return currentRound > MAX_ROUNDS;
    }

    /**
     * Force the game into the "over" state regardless of round progression.
     * Used by the controller when the suspension timeout proclaims a winner
     * or when the last connected player drops during a suspension and the
     * game has to end early.
     *
     * @apiNote @CalledOnGameThreadOnly
     */
    public void setGameOver() {
        this.currentRound = MAX_ROUNDS + 1;
    }

    /** @apiNote @CalledOnGameThreadOnly */
    public void setWinners(List<String> winnerNames) {
        this.winners = winnerNames;
    }

    // getters –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    public Board getBoard() {
        return board;
    }

    public RowsManager getRowsManager(){
        return rowsManager;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public GamePhaseHandler getPhaseHandler() {
        return currentPhaseHandler;
    }

    public GamePhase getCurrentPhase() {
        return currentPhaseHandler != null ? currentPhaseHandler.getPhase() : null;
    }

    public Player getCurrentPlayer() {
        return currentPhaseHandler != null ? currentPhaseHandler.getCurrentPlayer() : null;
    }

    public List<Player> getTurnOrder() {
        return board.getTurnOrder();
    }

    public Era getCurrentEra() {
        return rowsManager.getCurrentEra();
    }

    public Player getPlayerByName(String name) {
        return players.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No player named: " + name));
    }

    public List<String> getWinners() {
        return winners;
    }
}
