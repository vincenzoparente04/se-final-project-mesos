package model.buildingEffects.OnEventEffects;

import model.cards.charachterCards.ArtistCard;
import model.enums.Era;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtistBonusForCavePaintingsTest {

    @Test
    void applyOnCavePaintingsAddsFoodPerArtist() {
        Player player = new Player("p1");
        player.getTribe().addArtist(new ArtistCard(1, Era.ERA_I, 2));
        player.getTribe().addArtist(new ArtistCard(2, Era.ERA_I, 2));
        player.getTribe().addArtist(new ArtistCard(3, Era.ERA_I, 2));

        ArtistBonusForCavePaintings effect = new ArtistBonusForCavePaintings(1);
        effect.applyOnCavePaintings(player);

        assertEquals(3, player.getFood());
    }

    @Test
    void applyOnCavePaintingsDoesNothingWithoutArtists() {
        Player player = new Player("p1");

        ArtistBonusForCavePaintings effect = new ArtistBonusForCavePaintings(1);
        effect.applyOnCavePaintings(player);

        assertEquals(0, player.getFood());
    }
}

