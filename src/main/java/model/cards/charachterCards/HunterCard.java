package model.cards.charachterCards;

import model.player.Player;
import model.enums.Era;

public class HunterCard extends CharacterCard {
    private final boolean triggerIcon;

    public HunterCard(int id, Era era, int playerCount, boolean hasTriggerIcon) {
        super(id, era, playerCount);
        this.triggerIcon = hasTriggerIcon;
    }

    /**
     * @implNote Add an hunter to its list in the tribe of the player who picked this card and add the
     * food if the card has the trigger icon depending on the number of hunters the players has.
     * @param player
     */
    @Override
    public void registerToTribe(Player player) {
        player.getTribe().addHunter(this);

        if(triggerIcon){ player.addFood(player.getTribe().getHunterCount()); }
    }
}
