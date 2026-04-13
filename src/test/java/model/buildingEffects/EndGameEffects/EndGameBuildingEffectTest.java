package model.buildingEffects.EndGameEffects;

import model.cards.characterCards.HunterCard;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndGameBuildingEffectTest {

    @Test
    void registerSelfRegistersEffectInTribeEndGameList() {
        Player player = new Player("p1");
        EndGameBuildingEffect effect = new EndGameBuildingEffect(3, Tribe::getHunterCount);

        int before = player.getTribe().getEndGameBuildingEffects().size();
        effect.registerSelf(player);

        assertEquals(before + 1, player.getTribe().getEndGameBuildingEffects().size());
        assertTrue(player.getTribe().getEndGameBuildingEffects().contains(effect));
    }

    @Test
    void applyEffectAddsPrestigeUsingMultiplierAndGetter() {
        Player player = new Player("p1");
        player.getTribe().addHunter(new HunterCard(1, Era.ERA_I, 2, false, "test/front.png" , "test/front.png"));
        player.getTribe().addHunter(new HunterCard(2, Era.ERA_I, 2, false, "test/front.png" , "test/front.png"));

        EndGameBuildingEffect effect = new EndGameBuildingEffect(3, Tribe::getHunterCount);
        effect.applyEffect(player);

        assertEquals(6, player.getPrestigePoints());
    }

    @Test
    void applyEffectSupportsFlatBonus() {
        Player player = new Player("p1");
        EndGameBuildingEffect effect = new EndGameBuildingEffect(25, ignored -> 1);

        effect.applyEffect(player);

        assertEquals(25, player.getPrestigePoints());
    }

    @Test
    void applyEffectDoesNotAddPrestigeWhenGetterReturnsZero() {
        Player player = new Player("p1");
        EndGameBuildingEffect effect = new EndGameBuildingEffect(5, ignored -> 0);

        effect.applyEffect(player);

        assertEquals(0, player.getPrestigePoints());
    }
}
