package model.buildingEffects.OnEventEffects;

import model.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OnEventBuildingEffectTest {

    //register self tested in buildingCardTest

    @Test
    void baseOnEventEffectIsNoOp() {
        Player player = new Player("p1");
        OnEventBuildingEffect effect = new OnEventBuildingEffect();
        player.addFood(5);
        player.addPrestigePoints(7);

        int discount = effect.applyOnSustenance(player);
        effect.applyOnShamanicRitual(player);
        effect.applyOnCavePaintings(player);
        effect.applyOnHunt(player);

        assertEquals(0, discount);
        assertEquals(5, player.getFood());
        assertEquals(7, player.getPrestigePoints());
    }
}
