package model.phaseHandlers;

import database.*;
import model.GameModel;
import model.cards.buildingCards.buildingEffects.endGameEffects.EndGameBuildingEffect;
import model.enums.GamePhase;
import model.player.Player;
import model.player.Tribe;
import network.server.core.VirtualView;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;
import shared.dto.event.PlayerScoringDeltaDto;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the final phase of the game, executing the teardown sequence
 * and finalizing the match statistics.
 * <p>
 * This handler encapsulates the end-game routine which includes resolving all
 * remaining visible event cards, calculating a detailed breakdown of each player's
 * final prestige points (incorporating character types and building effects), and
 * determining the winner(s) using a food-based tie-breaking mechanism if necessary.
 * </p>
 * <p>
 * The behavior of this phase adapts dynamically based on its initialization state:
 * <ul>
 * <li><b>Standard Termination:</b> Processes full end-game scoring, broadcasts
 * event and scoring data to all clients, and asynchronously persists match
 * records to the database if the database layer is enabled.</li>
 * <li><b>Suspended Game (Abnormal Termination):</b> Triggered when all players disconnect.
 * It bypasses all scoring calculations and database
 * operations, immediate broadcasting a clean game-over status with empty scores
 * to ensure proper client-side cleanup.</li>
 * </ul>
 * </p>
 * @see GamePhaseHandler
 * @see database.MatchDAO
 * @see shared.dto.event.EndGameScoringDto
 */
public class EndOfGamePhase implements GamePhaseHandler {

    private final GameModel model;
    private List<Player> winners;
    private EndGameScoringDto scoring; // built in calculateEndGameScoring(), shipped from onEnter()
    private MatchDAO matchDAO;
    private boolean suspendedGame; // if true, it means that the game ended due to a player disconnection, so we skip end-game scoring and DB saving

    public EndOfGamePhase(GameModel model, List<Player> winners, boolean suspendedGame) {
        this.model = model;
        this.winners = winners;
        this.suspendedGame = suspendedGame;
    }

    /**
     * Initializes and executes the macro end-game sequence.
     * <p>
     * If the match concluded naturally, it triggers the resolution pipeline, computes final
     * scores, and delegates loading the database to an asynchronous worker thread. In case of
     * an abnormal suspension, it immediately broadcasts an empty scoring DTO to signal
     * a clean client-side teardown.
     * </p>
     */
    @Override
    public void onEnter() {
        if (!suspendedGame) {
            resolveAllVisibleEvents();
            calculateEndGameScoring();
            determineWinner();

            List<String> winnerNames = winners.stream().map(Player::getName).toList();
            model.setWinners(winnerNames);

            if (database.DatabaseManager.isEnabled()) {
                processDatabaseAsync(winnerNames);
            } else {
                for (VirtualView v : model.getViews()) {
                    v.sendGameOver(winnerNames, scoring);
                }
            }

        }else {

            List<String> winnerNames = winners.stream().map(Player::getName).toList();
            model.setWinners(winnerNames);
            scoring = new EndGameScoringDto(new ArrayList<>()); // empty scoring since the game was suspended, so no points are calculated

            for (VirtualView v : model.getViews()) {
                v.sendGameOver(winnerNames, scoring);
            }
        }
    }

    /**
     * Offloads database persistence and leaderboard retrieval to a dedicated background thread.
     * <p>
     * This asynchronous design prevents the main game loop and the networking server from
     * blocking during potentially slow I/O operations. Upon successful database interaction,
     * it dispatches the updated global standings and the final game-over payload to all clients.
     * </p>
     *
     * @param winnerNames the list of winning players' names, required to formulate
     * the broadcasted game-over message.
     */
    private void processDatabaseAsync(List<String> winnerNames) {
        this.matchDAO = new MatchDAO();

        List<ScoreRecord> recordsToSave = new ArrayList<>();
        for (Player p : model.getPlayers()) {
            recordsToSave.add(new ScoreRecord(
                    p.getName(),
                    p.getPrestigePoints(),
                    model.getPlayerCount(),
                    null
            ));
        }

        new Thread(() -> {
            try {
                matchDAO.saveMatch(recordsToSave);
                List<ScoreRecord> topStanding = matchDAO.getTopScores(model.getPlayerCount(), 20);

                for (Player p : model.getPlayers()) {
                    int standingPosition = matchDAO.getPlayerRank(model.getPlayerCount(), p.getName(), p.getPrestigePoints());

                    VirtualView playerView = model.getViews().stream()
                            .filter(v -> v.getPlayerName().equals(p.getName()))
                            .findFirst()
                            .orElse(null);

                    if (playerView != null) {
                        playerView.sendLeaderboard(topStanding, standingPosition, p.getPrestigePoints());
                    }
                }
                for (VirtualView v : model.getViews()) {
                    v.sendGameOver(winnerNames, scoring);
                }
            } catch (Exception e) {
                for (VirtualView v : model.getViews()) {
                    v.sendGameOver(winnerNames, scoring);
                    v.sendError("[DB] Error during async operation: " + e.getMessage());
                }
            }
        }, "db-async-thread").start();
    }

    /**
     * Resolves all remaining event cards currently present on both the top and bottom offer rows.
     * <p>
     * Deviating from standard round behavior, this ensures that all lingering game-state mutations
     * (e.g., final sustenance or specific end-game modifiers) are rigorously applied before
     * calculating the final prestige points. Each resolution broadcasts an update to the clients.
     * </p>
     */
    private void resolveAllVisibleEvents() {
        List<EventResolutionDto> resolutions = model.getRowsManager().resolveAllEvents(model.getPlayers());
        for (EventResolutionDto r : resolutions) {
            for (VirtualView v : model.getViews()) {
                v.sendEventResolved(r);
            }
        }
    }

    /**
     * Aggregates and calculates the final prestige points for every active player.
     * <p>
     * The calculation strategy incorporates disparate sources:
     * <ul>
     * <li>Character Card collections (Builders, Artists, Inventors).</li>
     * <li>Intrinsic points printed on acquired Building Cards.</li>
     * <li>Dynamic modifiers from active {@link EndGameBuildingEffect} instances.</li>
     * </ul>
     * </p>
     * <p>
     * The results are structured into an {@link EndGameScoringDto} to provide the front-end
     * with a transparent, per-category breakdown of how the final scores were achieved.
     * </p>
     */
    private void calculateEndGameScoring() {
        List<PlayerScoringDeltaDto> deltas = new ArrayList<>();

        for (Player player : model.getPlayers()) {
            Tribe tribe = player.getTribe();
            int prestigeBefore = player.getPrestigePoints();

            int buildersPts        = tribe.calculateBuildersEndGamePoints();
            int artistsPts         = tribe.calculateArtistEndGamePoints();
            int inventorsPts       = tribe.calculateInventorEndGamePoints();
            int buildingPrintedPts = tribe.calculateBuildingPrintedPoints();

            player.addPrestigePoints(buildersPts);
            player.addPrestigePoints(artistsPts);
            player.addPrestigePoints(inventorsPts);
            player.addPrestigePoints(buildingPrintedPts);

            int prestigeBeforeEffects = player.getPrestigePoints();
            for (EndGameBuildingEffect effect : tribe.getEndGameBuildingEffects()) {
                effect.applyEffect(player);
            }
            int endGameEffectsPts = player.getPrestigePoints() - prestigeBeforeEffects;

            int prestigeAfter = player.getPrestigePoints();
            String details = "Builders %+d, Artists %+d, Inventors %+d, Buildings %+d, Effects %+d"
                    .formatted(buildersPts, artistsPts, inventorsPts, buildingPrintedPts, endGameEffectsPts);

            deltas.add(new PlayerScoringDeltaDto(
                    player.getName(),
                    prestigeBefore, prestigeAfter,
                    buildersPts, artistsPts, inventorsPts,
                    buildingPrintedPts, endGameEffectsPts,
                    details));
        }

        this.scoring = new EndGameScoringDto(deltas);
    }

    /**
     * Evaluates the final computed scores to identify the match winner(s), applying
     * domain-specific tie-breaking rules.
     * <p>
     * The primary metric is the total accumulated prestige points. In the event of a tie,
     * the system defers to the total food reserves held by the tied players. If food reserves
     * are also identical, the victory is officially shared among them.
     * </p>
     */
    private void determineWinner() {
        List<Player> players = model.getPlayers();

        int maxPrestige = players.stream()
                .mapToInt(Player::getPrestigePoints)
                .max()
                .orElse(0);

        List<Player> tied = players.stream()
                .filter(p -> p.getPrestigePoints() == maxPrestige)
                .toList();

        if (tied.size() == 1) {
            winners = tied;
            return;
        }

        int maxFood = tied.stream()
                .mapToInt(Player::getFood)
                .max()
                .orElse(0);

        winners = tied.stream()
                .filter(p -> p.getFood() == maxFood)
                .toList();
    }


    public List<Player> getWinners() {
        return winners;
    }

    @Override
    public GamePhase getPhase() { return GamePhase.END_OF_GAME; }

    @Override
    public Player getCurrentPlayer() { return null; }
}
