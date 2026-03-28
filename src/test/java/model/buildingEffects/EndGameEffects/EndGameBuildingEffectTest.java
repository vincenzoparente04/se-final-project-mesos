package model.buildingEffects.EndGameEffects;

import model.cards.charachterCards.HunterCard;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EndGameBuildingEffectTest {
    
    //the register self test is done in the BuildingCardTest

    @Test
    void applyEffectAddsPrestigeUsingMultiplierAndGetter() {
        Player player = new Player("p1");
        player.getTribe().addHunter(new HunterCard(1, Era.ERA_I, 2, false));
        player.getTribe().addHunter(new HunterCard(2, Era.ERA_I, 2, false));

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
