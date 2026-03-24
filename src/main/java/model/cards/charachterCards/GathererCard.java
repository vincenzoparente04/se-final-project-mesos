package model.cards.charachterCards;

import model.enums.Era;

public class GathererCard extends CharacterCard {
    public GathererCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
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