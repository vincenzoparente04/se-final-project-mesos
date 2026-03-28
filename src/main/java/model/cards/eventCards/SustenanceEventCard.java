package model.cards.eventCards;

import model.board.CardVisitor;
import model.buildingEffects.OnEventEffects.OnEventBuildingEffect;
import model.enums.Era;
import model.player.Player;

import java.util.List;

public class SustenanceEventCard extends EventCard {

    public SustenanceEventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    
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
            int discount = p.getTribe().getTotalGatherersDiscount();

            // building effects: accumula sconto extra
            for (OnEventBuildingEffect effect : p.getTribe().getOnEventBuildingEffects()) {
                discount += effect.applyOnSustenance(p);
            }

            int foodToPay = Math.max(0, characterCount - discount);
            p.removeFood(foodToPay, (this.getEra().ordinal() + 1));
        });
    }

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}