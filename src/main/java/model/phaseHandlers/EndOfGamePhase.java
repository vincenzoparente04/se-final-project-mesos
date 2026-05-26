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

public class EndOfGamePhase implements GamePhaseHandler {

    private final GameModel model;
    private List<Player> winners;
    private EndGameScoringDto scoring; // built in calculateEndGameScoring(), shipped from onEnter()
    private MatchDAO matchDAO; // initialized in onEnter() after DatabaseManager sets up the DB
    private boolean dbOn;

    public EndOfGamePhase(GameModel model) {
        this.model = model;
    }

    /**
     * @implNote  This method resolves all visible events, calculates end-game scoring for each player,
     * determines the winner, and notifies observers of the game over state and the winner(s).
     */
    @Override
    public void onEnter() {
        resolveAllVisibleEvents();
        calculateEndGameScoring();
        determineWinner();

        List<String> winnerNames = winners.stream().map(Player::getName).toList();
        model.setWinners(winnerNames);

        dbOn = updateDB(); //TODO DA USARE PER CAPIRE SE MOSTRARE IL PULSANTE PER RICEVERE LA CLASSIFICA
        getStandingPosition();


        // Explicit game-over broadcast carrying both winners and scoring
        // (the state message no longer auto-emits the GameOverMessage).
        for (VirtualView v : model.getViews()) {
            v.sendGameOver(winnerNames, scoring);
        }
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

    public boolean updateDB() {
        try {
            DatabaseManager.initializeDatabase(model.getDbConfig());
            this.matchDAO = new MatchDAO();

            List<ScoreRecord> recordsToSave = new ArrayList<>();
            for (Player p : model.getPlayers()) {
                //TODO capire come vogliamo calcolare i punti
                boolean isWinner = winners.contains(p);
                recordsToSave.add(new ScoreRecord(p.getName(), isWinner, model.getPlayerCount()));
            }
            // Synchronized call. Game thread stop here until db end updating
            matchDAO.saveMatch(recordsToSave);
            return true;
        } catch (SQLException e) {
            System.err.println("Database error during db update: " + e.getMessage());
            return false;
        }
    }

    public void getStandingPosition(){
        // top 10
        List<ScoreRecord> topTen = matchDAO.getTopScores(model.getPlayerCount(), 10);
        System.out.println("Top Ten: " + topTen);

        // player standing position
        for (Player p : model.getPlayers()) {
            int standingPosition = matchDAO.getPlayerRank(model.getPlayerCount(), p.getName());

            // Ora hai tutto ciò che serve:
            // - 'topTen' (la classifica da mostrare a schermo)
            // - 'posizioneAssoluta' (il posizionamento specifico di questo singolo player)

            //TODO PROVVISIORIO PER TESTARE
            System.out.println(p.getName() + " -> Classifica ottenuta. La tua posizione è: " + standingPosition + "°");
        }
    }

    public List<Player> getWinners() {
        return winners;
    }

    @Override
    public GamePhase getPhase() { return GamePhase.END_OF_GAME; }

    @Override
    public Player getCurrentPlayer() { return null; }
}
