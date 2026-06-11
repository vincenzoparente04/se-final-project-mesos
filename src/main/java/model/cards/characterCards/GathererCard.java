package model.cards.characterCards;

import model.enums.Era;

public class GathererCard extends CharacterCard {
    public GathererCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * Registers this card by adding a gatherer to the player's tribe.
     *
     * @param player the player who drew this card
     */
    @Override
    public void registerToTribe(model.player.Player player) {
        player.getTribe().addGatherer(this);
    }
}