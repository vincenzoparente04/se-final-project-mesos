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
     * @implNote  This method resolves all visible events, calculates end-game scoring for each player,
     * determines the winner, and notifies observers of the game over state and the winner(s).
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
            List<String> winnerNames = null;
            if (winners != null) {
                winnerNames = winners.stream().map(Player::getName).toList();
                model.setWinners(winnerNames);
                scoring = new EndGameScoringDto(new ArrayList<>()); // empty scoring since the game was suspended, so no points are calculated
            }

            for (VirtualView v : model.getViews()) {
                v.sendGameOver(winnerNames, scoring);
            }
        }
    }

    /**
     * This method handle match scores saving and standings receiving with a separate thread to avoid server block
     * @param winnerNames needed to send game over messages
     * @implNote it creates a list of {@link ScoreRecord}, one for each player.
     * Then a thread starts, and it updates the db, get the top of the standing and the player match score position.
     * Last, for each client, it sends a {@link shared.message.LeaderboardMessage} to notify the client with the updated standings and it sends a {@link shared.message.GameOverMessage}.
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
     * @implNote Unlike normal rounds, the final round resolves events
     * from BOTH the top and bottom rows. One {@code EventResolvedMessage}
     * is broadcast per resolved card so the client can show what happened.
     * Sustenance must be resolved last as usual.
     */
    private void resolveAllVisibleEvents() {
        List<EventResolutionDto> resolutions = model.getRowsManager().resolveAllEvents(model.getPlayers());
        for (EventResolutionDto r : resolutions) {
            for (VirtualView v : model.getViews()) {
                v.sendEventResolved(r);
            }
        }
        // model.notifyChange();
    }

    /**
     * @implNote  Calculates end-game prestige points for each player. Builds
     * the {@link EndGameScoringDto} with the per-player breakdown so it can
     * be shipped together with the winners in the {@code GameOverMessage}.
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
        // model.notifyChange();
    }

    /**
     * @implNote  Determines the winner. In case of prestige points tie, it chooses between who has more food,
     * and if still tied, it's a shared victory.
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

        // in case of tie calculates who has more food
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
