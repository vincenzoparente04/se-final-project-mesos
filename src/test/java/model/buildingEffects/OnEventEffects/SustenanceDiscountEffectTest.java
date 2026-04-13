package model.buildingEffects.OnEventEffects;

import model.cards.characterCards.ArtistCard;
import model.cards.characterCards.HunterCard;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SustenanceDiscountEffectTest {

    @Test
    void applyOnSustenanceUsesGivenGetterAndMultiplier() {
        Player player = new Player("p1");
        player.getTribe().addArtist(new ArtistCard(1, Era.ERA_I, 2, "test/front.png" , "test/front.png"));
        player.getTribe().addArtist(new ArtistCard(2, Era.ERA_I, 2, "test/front.png" , "test/front.png"));
        player.getTribe().addHunter(new HunterCard(3, Era.ERA_I, 2, false, "test/front.png" , "test/front.png"));

        SustenanceDiscountEffect effect = new SustenanceDiscountEffect(3, Tribe::getArtistCount);

        assertEquals(6, effect.applyOnSustenance(player));
    }

    @Test
    void applyOnSustenanceReturnsZeroWhenNoMatchingCharacters() {
        Player player = new Player("p1");
        SustenanceDiscountEffect effect = new SustenanceDiscountEffect(4, Tribe::getShamanCount);

        assertEquals(0, effect.applyOnSustenance(player));
    }
}

