package model.cards.charachterCards;

import model.enums.Era;
import model.enums.InventionIcon;

public class InventorCard extends CharacterCard {
    private final InventionIcon inventionIcon;

    public InventorCard(int id, Era era, int playerCount, InventionIcon inventionIcon) {
        super(id, era, playerCount);
        this.inventionIcon = inventionIcon;
    }

    public InventionIcon getInventionIcon() {
        return inventionIcon;
    }

    @Override
    public void registerToTribe(model.player.Player player) {
        player.getTribe().addInventor(this);
    }
}
