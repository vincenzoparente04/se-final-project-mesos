package model.buildingEffects.OnEventEffects;

import model.cards.buildingCards.buildingEffects.onEventEffects.HunterBonusForHuntEvent;
import model.cards.characterCards.HunterCard;
import model.enums.Era;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HunterBonusForHuntEventTest {

    @Test
    void applyOnHuntAddsFoodAndPrestigePerHunter() {
        Player player = new Player("p1");
        player.getTribe().addHunter(new HunterCard(1, Era.ERA_I, 2, false, "test/front.png" , "test/front.png"));
        player.getTribe().addHunter(new HunterCard(2, Era.ERA_I, 2, false, "test/front.png" , "test/front.png"));

        HunterBonusForHuntEvent effect = new HunterBonusForHuntEvent(1, 1, "");
        effect.applyOnHunt(player);

        assertEquals(2, player.getFood());
        assertEquals(2, player.getPrestigePoints());
    }

    @Test
    void applyOnHuntDoesNothingWithoutHunters() {
        Player player = new Player("p1");

        HunterBonusForHuntEvent effect = new HunterBonusForHuntEvent(1, 1, "");
        effect.applyOnHunt(player);

        assertEquals(0, player.getFood());
        assertEquals(0, player.getPrestigePoints());
    }
}

