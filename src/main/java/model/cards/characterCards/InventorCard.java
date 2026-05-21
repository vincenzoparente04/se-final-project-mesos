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
     * @implNote Add an inventor to its list in the tribe of the player who picked this card.
     * @param player
     */
    @Override
    public void registerToTribe(model.player.Player player) {
        player.getTribe().addInventor(this);
    }
}
