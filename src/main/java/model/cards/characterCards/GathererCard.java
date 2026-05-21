package model.cards.characterCards;

import model.enums.Era;

public class GathererCard extends CharacterCard {
    public GathererCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * @implNote Add a gatherer to its list in the tribe of the player who picked this card.
     * @param player
     */
    @Override
    public void registerToTribe(model.player.Player player) {
        player.getTribe().addGatherer(this);
    }
}