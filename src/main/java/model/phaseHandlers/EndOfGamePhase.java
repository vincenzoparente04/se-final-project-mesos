package model.phaseHandlers;

import model.GameModel;
import model.buildingEffects.EndGameEffects.EndGameBuildingEffect;
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

    /**
     * @implNote  This method resolves all visible events, calculates end-game scoring for each player,
     * determines the winner, and notifies observers of the game over state and the winner(s).
     */
    @Override
    public void onEnter() {
        resolveAllVisibleEvents();
        calculateEndGameScoring();
        determineWinner();

        model.notifyChange("game_over:" + formatWinners());
    }

    /**
     * @implNote Unlike normal rounds, the final round resolves events
     * from BOTH the top and bottom rows.
     * Sustenance must be resolved last as usual.
     */
    private void resolveAllVisibleEvents() {
        model.getBoard().resolveAllEvents(model.getPlayers());
        model.notifyChange("final_events_resolved");
    }

    /**
     * @implNote  Calculates end-game prestige points for each player.
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

            // end game builging effects
            for (EndGameBuildingEffect effect : player.getTribe().getEndGameBuildingEffects()) {
                effect.applyEffect(player);
            }
        }

        model.notifyChange("endgame_scoring_complete");
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

    /**
     * @implNote Returns the name(s) of the winner(s)
     * @return Winners's name
     */
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
    public GamePhase getPhase() { return GamePhase.END_OF_GAME; }

    @Override
    public Player getCurrentPlayer() { return null; }
}