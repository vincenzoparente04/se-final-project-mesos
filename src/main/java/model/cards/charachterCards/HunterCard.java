package model.cards.charachterCards;

import model.player.Player;
import model.enums.Era;

public class HunterCard extends CharacterCard {
    private final boolean triggerIcon;

    public HunterCard(int id, Era era, int playerCount, boolean hasTriggerIcon) {
        super(id, era, playerCount);
        this.triggerIcon = hasTriggerIcon;
    }

    @Override
    public void registerToTribe(Player player) {
        player.getTribe().addHunter(this);

        if(triggerIcon){ player.addFood(player.getTribe().getHunterCount()); }
    }
}
