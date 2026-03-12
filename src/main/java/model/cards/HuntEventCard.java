package model.cards;

import model.enums.CardType;
import model.enums.CharacterType;
import model.player.Tribe;
import model.player.Player;

import java.util.List;

public class HuntEventCard extends EventCard{
    /**
     *
     * @param players
     */
    @Override
    public void resolve(List<Player> players) {
        players.forEach( p -> {
            int hunters = p.getTribe().countByType(CharacterType.HUNTER);
            p.addFood(hunters);
            p.addPrestigePoints(hunters * this.getEra().ordinal());
        });
    }

    @Override
    public CardType getCardType() {
        return null;
    }
}
