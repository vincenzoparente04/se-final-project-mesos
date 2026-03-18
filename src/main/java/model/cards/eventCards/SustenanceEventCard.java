package model.cards.eventCards;

import model.board.CardVisitor;
import model.enums.CardType;
import model.player.Player;

import java.util.List;

public class SustenanceEventCard extends EventCard {
    /**
     * Resolves the sustenance event for all players in the game.
     * * @param players The list of players participating in the event.
     * @implNote The method iterates over the players, calculating the food requirement
     * as the difference between the total character count and the gatherer discounts.
     * It then delegates the penalty management to the {@code removeFood} method
     * of the {@code Player} class, using the current era as the penalty multiplier.
     */
    @Override
    public void resolve(List<Player> players) {
        players.forEach( p -> {
            int characterCount = p.getTribe().getTotalCharacterCount();
            int foodToPay = characterCount - p.getTribe().getTotalGatherersDiscount();

            p.removeFood(foodToPay, this.getEra().ordinal());
        });
    }

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}