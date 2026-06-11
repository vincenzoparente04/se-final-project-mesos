package model.cards.characterCards;

import model.enums.Era;
import model.player.Player;

public class HunterCard extends CharacterCard {
    private final boolean triggerIcon;

    public HunterCard(int id, Era era, int playerCount, boolean hasTriggerIcon, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
        this.triggerIcon = hasTriggerIcon;
    }

    /**
     * Registers this card by adding a hunter to the player's tribe.
     * If this card carries a trigger icon, immediately grants the player
     * food equal to their updated hunter count.
     *
     * @param player the player who drew this card
     */
    @Override
    public void registerToTribe(Player player) {
        player.getTribe().addHunter(this);

        if(triggerIcon){ player.addFood(player.getTribe().getHunterCount()); }
    }

    public boolean hasTriggerIcon() { return triggerIcon; }
}
