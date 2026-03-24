package model.cards.charachterCards;

import model.enums.Era;
import model.player.Player;

public class ArtistCard extends CharacterCard {
    public ArtistCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }

    /**
     * @implNote Add an artist to its list in the tribe of the player who picked this card.
     * @param player
     */
    @Override
    public void registerToTribe(Player player) {
            player.getTribe().addArtist(this);
    }
}
