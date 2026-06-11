package model.cards.characterCards;

import model.enums.Era;
import model.player.Player;

public class ArtistCard extends CharacterCard {
    public ArtistCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * Registers this card by adding an artist to the player's tribe.
     *
     * @param player the player who drew this card
     */
    @Override
    public void registerToTribe(Player player) {
            player.getTribe().addArtist(this);
    }
}
