package model.cards.charachterCards;

import model.enums.Era;
import model.player.Player;

public class ArtistCard extends CharacterCard {
    public ArtistCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }

    @Override
    public void registerToTribe(Player player) {
            player.getTribe().addArtist(this);
    }
}
