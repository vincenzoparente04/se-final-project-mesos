package model.cards.eventCards;

import model.buildingEffects.OnEventEffects.OnEventBuildingEffect;
import model.enums.Era;
import model.player.Player;

import java.util.List;

public class HuntEventCard extends EventCard {

    public HuntEventCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }

    /**
     * @implNote For each player, add to their food the number of hunters they have and add prestige points
     * equal to the number of hunters multiplied by the era of the card (ERA_I = 1, ERA_II = 2, ERA_III = 3).
     * Calls if eventually there are buildings who affect the hunter event.
     * @param players
     */
    @Override
    public void resolve(List<Player> players) {
        players.forEach( p -> {
            int hunters = p.getTribe().getHunterCount();
            p.addFood(hunters);
            p.addPrestigePoints(hunters * (this.getEra().ordinal() + 1));

            // building effects
            for (OnEventBuildingEffect effect : p.getTribe().getOnEventBuildingEffects()) {
                effect.applyOnHunt(p);
            }
        });
    }
}
