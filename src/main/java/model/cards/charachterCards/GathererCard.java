package model.cards.charachterCards;

import model.enums.Era;

public class GathererCard extends CharacterCard {
    public GathererCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }

    @Override
    public void registerToTribe(model.player.Player player) {
        player.getTribe().addGatherer(this);
    }
}