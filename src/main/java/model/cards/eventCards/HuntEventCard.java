package model.cards.eventCards;

import model.buildingEffects.OnEventEffects.OnEventBuildingEffect;
import model.enums.CardType;
import model.enums.CharacterType;
import model.player.Player;

import java.util.List;

public class HuntEventCard extends EventCard {

    public HuntEventCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }

    /**
     *
     * @param players
     */
    @Override
    public void resolve(List<Player> players) {
        players.forEach( p -> {
            int hunters = p.getTribe().getHunterCount();
            p.addFood(hunters);
            p.addPrestigePoints(hunters * this.getEra().ordinal());

            // building effects
            for (OnEventBuildingEffect effect : p.getTribe().getOnEventBuildingEffects()) {
                effect.applyOnHunt(p);
            }
        });
    }
}
