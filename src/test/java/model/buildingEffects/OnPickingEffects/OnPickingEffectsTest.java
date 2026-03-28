package model.buildingEffects.OnPickingEffects;

import model.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OnPickingEffectsTest {

    @Test
    void registerSelfSetsShamanicImmunity() {
        Player player = new Player("p1");
        OnPickingEffects effect = new OnPickingEffects(p -> p.setShamanicImmunity(true));

        effect.registerSelf(player);

        assertTrue(player.hasShamanicImmunity());
    }

    @Test
    void registerSelfSetsShamanicBonusIcons() {
        Player player = new Player("p1");
        OnPickingEffects effect = new OnPickingEffects(p -> p.setShamanicBonusIcons(true));

        effect.registerSelf(player);

        assertTrue(player.hasShamanicBonusIcons());
    }

    @Test
    void registerSelfSetsShamanicDoublePrestige() {
        Player player = new Player("p1");
        OnPickingEffects effect = new OnPickingEffects(p -> p.setShamanicDoublePrestige(true));

        effect.registerSelf(player);

        assertTrue(player.hasShamanicDoublePrestige());
    }

    @Test
    void registerSelfSetsExtraDraw() {
        Player player = new Player("p1");
        OnPickingEffects effect = new OnPickingEffects(p -> p.setExtraDraw(true));

        effect.registerSelf(player);

        assertTrue(player.hasExtraDraw());
    }

    @Test
    void registerSelfSetsExtraFoodOnTotemReturn() {
        Player player = new Player("p1");
        OnPickingEffects effect = new OnPickingEffects(p -> p.setExtraFoodOnTotemReturn(true));

        effect.registerSelf(player);

        assertTrue(player.hasExtraFoodOnTotemReturn());
    }
}
