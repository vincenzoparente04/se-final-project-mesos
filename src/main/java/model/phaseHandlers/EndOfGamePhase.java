package model.phaseHandlers;

import model.GameModel;
import model.cards.eventCards.EventCard;
import model.enums.GamePhase;
import model.player.Player;
import model.player.Tribe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EndOfGamePhase extends GamePhaseHandler {

    private Player winner;
    private List<Player> winners; // in case it's a draw

    public EndOfGamePhase(GameModel model) {
        super(model);
    }

    @Override
    public void onEnter() {
        resolveAllVisibleEvents();
        calculateEndGameScoring();
        determineWinner();

        model.notifyChange("game_over:" + formatWinners());
    }

    /**
     * Unlike normal rounds, the final round resolves events
     * from BOTH the top and bottom rows.
     * Sustenance must be resolved last as usual.
     */
    private void resolveAllVisibleEvents() {
       // TODO a Filippone pensace te
        model.getBoard().resolveAllEvents(model.getPlayers());
        model.notifyChange("final_events_resolved");
    }

    /**
     * Calculates end-game prestige points for each player.
     * Each scoring source is handled by the Tribe, which already
     * has the typed lists and query methods needed.
     */
    private void calculateEndGameScoring() {
        for (Player player : model.getPlayers()) {
            Tribe tribe = player.getTribe();

            // PP from Builders
            player.addPrestigePoints(tribe.calculateBuildersEndGamePoints());

            // 10 PP for every 2 Artists
            player.addPrestigePoints(tribe.calculateArtistEndGamePoints());

            // Inventors × distinct invention icons
            player.addPrestigePoints(tribe.calculateInventorEndGamePoints());

            // Buildings: printed PP + endgame effects
            player.addPrestigePoints(tribe.calculateBuildingPrintedPoints());

            // TODO aggiungere la chiamata al metodo della tribe che calcola gli effetti EndGame dei building
        }

        model.notifyChange("endgame_scoring_complete");
    }

    /**
     * Determines the winner.
     * Tiebreak: most Food. If still tied, victory is shared.
     */
    // TODO: da controllare
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

        // tiebreak: chi ha più Food
        int maxFood = tied.stream()
                .mapToInt(Player::getFood)
                .max()
                .orElse(0);

        winners = tied.stream()
                .filter(p -> p.getFood() == maxFood)
                .toList();

        // se ancora pari, la vittoria è condivisa — winners contiene più giocatori
    }

    private String formatWinners() {
        if (winners.size() == 1) {
            return winners.getFirst().getName();
        }
        return winners.stream()
                .map(Player::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    public List<Player> getWinners() {
        return winners;
    }

    @Override
    public GamePhase getPhase() {
        return model.getCurrentPhase();
    }

    @Override
    public Player getCurrentPlayer() {
        return null;
    }
}