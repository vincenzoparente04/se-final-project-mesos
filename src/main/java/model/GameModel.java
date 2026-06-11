package model;

import model.board.Board;
import model.enums.Era;
import model.enums.GamePhase;
import model.phaseHandlers.ColorChoosingPhase;
import model.phaseHandlers.GamePhaseHandler;
import database.ScoreRecord;
import model.player.Player;
import model.rowsManager.RowsManager;
import network.server.core.VirtualView;
import shared.command.gameCommand.GameCommand;
import shared.dto.GameStateDto;
import shared.dto.event.EndGameScoringDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Authoritative game state. Owns the players, the board, the row manager and
 * the current phase handler, and is responsible for notifying its registered
 * {@link VirtualView}s when the state changes.
 *
 * <h2>Threading contract</h2>
 * After construction, {@code GameModel} is mutated primarily by the
 * single <em>game thread</em> owned by {@code GameController}. All mutators
 * documented as {@code @CalledOnGameThreadOnly} below MUST NOT be invoked
 * from network/handler threads. However, there is one exception: during the
 * {@link model.phaseHandlers.ColorChoosingPhase ColorChoosingPhase}, if the
 * 60-second timeout expires, an asynchronous timeout thread may call 
 * {@link #notifyChange()} to apply random color assignments to inactive players
 * and transition to the {@link model.phaseHandlers.SetupPhase SetupPhase}.
 * 
 * Read-only getters that are accessed across threads (e.g. {@link #isGameOver()})
 * are made safe by marking the underlying fields {@code volatile}.
 */
public class GameModel {
    private static final int MAX_ROUNDS = 10;

    private Board board;
    private final RowsManager rowsManager = new RowsManager();
    private List<Player> players = new ArrayList<>();
    private volatile int currentRound = 1;
    private List<String> winners = new ArrayList<>();

    /**
     * End-game results owned by the model so they survive a reconnection and can
     * be re-sent on demand (see {@link #notifyEndGame()}).
     * <p>
     * {@code endGameScoring} is written only on the game thread (in
     * {@code EndOfGamePhase.onEnter}, before the DB thread is even started) and is
     * {@code null} for the suspension/forfeit case. {@code leaderboard} is the only
     * end-game field written off the game thread (by the async DB thread): it is
     * published as a single immutable {@link LeaderboardData} snapshot through a
     * {@code volatile} reference, so a reader sees either {@code null} or the fully
     * built snapshot — never a partial state.
     */
    private EndGameScoringDto endGameScoring;
    private volatile LeaderboardData leaderboard;

    /** Immutable snapshot of the DB leaderboard, atomically publishable. */
    public record LeaderboardData(List<ScoreRecord> top, Map<String, Integer> rankByName) {}

    private volatile GamePhaseHandler currentPhaseHandler;
    private final List<VirtualView> views;

    /**
     * Constructs a game model with the specified list of views.
     *
     * @param views the list of {@link VirtualView}s representing connected clients
     */
    public GameModel(List<VirtualView> views) {
        this.views = new ArrayList<>(views);
    }

    /**
     * Constructs a game model without any registered views.
     * 
     * Convenience constructor for tests that don't need any view registered.
     */
    public GameModel() {
        this(List.of());
    }

    /**
     * Initializes the game constructing the board and creating the players and starting the color-choosing phase.
     *
     * @param playerNames the list of player names
     */
    public void startGame(List<String> playerNames) {
        this.board = new Board(playerNames.size());
        createPlayers(playerNames);
        setPhase(new ColorChoosingPhase(this));
    }

    /**
     * Creates player objects from the given list of names.
     *
     * @param playerNames the list of player names to create players from
     */
    private void createPlayers(List<String> playerNames) {
        this.players = new ArrayList<>();
        for (String name : playerNames) {
            this.players.add(new Player(name));
        }
    }

    /**
     * Transitions the game to a new phase and triggers its entry logic.
     *
     * @param phase the new {@link GamePhaseHandler} to activate
     */
    public void setPhase(GamePhaseHandler phase) {
        this.currentPhaseHandler = phase;
        phase.onEnter();
    }

    /**
     * Single dispatch point for all in-game commands. Snapshots the current
     * phase handler once and visits the command on it: the handler decides,
     * via polymorphic dispatch on the command type, whether and how to
     * execute it.
     */
    public void handleCommand(GameCommand cmd) throws Exception {
        GamePhaseHandler handler = this.currentPhaseHandler;
        cmd.accept(handler);
    }

    /**
     * broadcast the updated state to every registered view. Called by the phase handlers after mutating the model.
     * <p>
     * It first creates a new {@link GameStateDto}. Then send the state to every {@link VirtualView} in the list of views.
     * The list is copied to avoid concurrent modification issues if a view is added/removed while iterating.
     * <p>
     * The model notify itself the views, avoiding the model to know the controller.
     * These actions are not blocking for server because {@link VirtualView} use a separate thread to send dto.
     */
    public void notifyChange() {
        GameStateDto dto = GameStateDtoBuilder.build(this);
        for (VirtualView v : new ArrayList<>(views)) {
            v.sendState(dto);
        }
    }

    /**
     * Broadcast the end-game payload to every registered view: the (personalised)
     * leaderboard if available, followed by the game-over with the scoring
     * breakdown. Re-sent on reconnection so a returning player rebuilds the final
     * screen. Performs no mutation (reads fields + I/O on a defensive copy of the
     * views), so it is safe to call from the game thread or the async DB thread.
     */
    public void notifyEndGame() {
        LeaderboardData leaderboard = this.leaderboard;
        for (VirtualView v : new ArrayList<>(views)) {
            if (leaderboard != null) {
                int rank = leaderboard.rankByName().getOrDefault(v.getPlayerName(), 0);
                int points = prestigeByName(v.getPlayerName());
                v.sendLeaderboard(leaderboard.top(), rank, points);
            }
            v.sendGameOver(winners, endGameScoring);
        }
    }

    private int prestigeByName(String name) {
        return players.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .map(Player::getPrestigePoints)
                .orElse(0);
    }

    /**
     * Replaces the view associated with the given player name.
     * 
     * If no view exists for that player, the new view is appended to the list.
     *
     * @param playerName the name of the player whose view is being replaced
     * @param newView the new {@link VirtualView} to associate with the player
     */
    public void swapView(String playerName, VirtualView newView) {
        views.removeIf(v -> v.getPlayerName().equals(playerName));
        views.add(newView);
    }

    /**
     * Removes the view associated with the given player name.
     *
     * @param playerName the name of the player whose view is being removed
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

    public void incrementRound() {
        currentRound++;
    }

    /**
     * Determines whether the game has ended based on round progression.
     *
     * @return true if the current round exceeds the maximum, false otherwise
     */
    public boolean isGameOver() {
        return currentRound > MAX_ROUNDS;
    }

    /**
     * Force the game into the "over" state regardless of round progression.
     * Used by the controller when the suspension timeout proclaims a winner
     * or when the last connected player drops during a suspension and the
     * game has to end early.
     */
    public void setGameOver() {
        this.currentRound = MAX_ROUNDS + 1;
    }

    public void setWinners(List<String> winnerNames) {
        this.winners = winnerNames;
    }

    /**
     * Sets the end-game scoring data.
     *
     * @param scoring the {@link EndGameScoringDto} containing final score details
     */
    public void setEndGameScoring(EndGameScoringDto scoring) {
        this.endGameScoring = scoring;
    }

    /**
     * Returns the end-game scoring data.
     *
     * @return the {@link EndGameScoringDto}, or null if not yet available
     */
    public EndGameScoringDto getEndGameScoring() {
        return endGameScoring;
    }

    /**
     * Publish the DB leaderboard snapshot. May be called from the async DB thread;
     * the {@code volatile} field guarantees a safe hand-off to the game thread.
     */
    public void setLeaderboard(LeaderboardData data) {
        this.leaderboard = data;
    }

    /**
     * Returns the leaderboard snapshot.
     *
     * @return the {@link LeaderboardData} published by the database thread, or null if not yet available
     */
    public LeaderboardData getLeaderboard() {
        return leaderboard;
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

    /**
     * Returns the current game phase.
     *
     * @return the {@link GamePhase} of the current handler, or null if none is active
     */
    public GamePhase getCurrentPhase() {
        return currentPhaseHandler != null ? currentPhaseHandler.getPhase() : null;
    }

    /**
     * Returns the player whose turn it currently is.
     *
     * @return the current active {@link Player}, or null if the current phase does not have an active player
     */
    public Player getCurrentPlayer() {
        return currentPhaseHandler != null ? currentPhaseHandler.getCurrentPlayer() : null;
    }

    public List<Player> getTurnOrder() {
        return board.getTurnOrder();
    }

    public Era getCurrentEra() {
        return rowsManager.getCurrentEra();
    }

    /**
     * Finds a player by their unique name.
     *
     * @param name the name of the player to find
     * @return the {@link Player} with the given name
     * @throws IllegalArgumentException if no player with the given name exists
     */
    public Player getPlayerByName(String name) {
        return players.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No player named: " + name));
    }

    /**
     * Returns the list of winning player names.
     *
     * @return a list of names of players who won the match
     */
    public List<String> getWinners() {
        return winners;
    }
}
