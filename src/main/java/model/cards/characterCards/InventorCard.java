package model.cards.characterCards;

import model.enums.Era;
import model.enums.InventionIcon;

public class InventorCard extends CharacterCard {
    private final InventionIcon inventionIcon;

    public InventorCard(int id, Era era, int playerCount, InventionIcon inventionIcon, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
        this.inventionIcon = inventionIcon;
    }

    public InventionIcon getInventionIcon() { return inventionIcon; }

    /**
     * Registers this card by adding an inventor to the player's tribe.
     *
     * @param player the player who drew this card
     */
    @Override
    public void registerToTribe(model.player.Player player) {
        player.getTribe().addInventor(this);
    }
}
