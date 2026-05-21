package model.buildingEffects.OnCharacterAcquiredEffects;

import model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects.OnAcquireBuildingEffect;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnAcquireBuildingEffectTest {

    @Test
    void registerSelfRegistersEffectInTribeOnAcquireList() {
        Player player = new Player("p1");
        OnAcquireBuildingEffect effect = new DummyOnAcquireEffect();

        int before = player.getTribe().getOnAcquireBuildingEffects().size();
        effect.registerSelf(player);

        assertEquals(before + 1, player.getTribe().getOnAcquireBuildingEffects().size());
        assertTrue(player.getTribe().getOnAcquireBuildingEffects().contains(effect));
    }

    private static class DummyOnAcquireEffect extends OnAcquireBuildingEffect {
        @Override
        public void applyEffect(Player player) {
            // no-op: this test only verifies registerSelf contract
        }
    }
}

