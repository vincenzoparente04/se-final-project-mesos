package model.buildingEffects.OnCharacterAcquiredEffects;

import model.cards.charachterCards.InventorCard;
import model.enums.Era;
import model.enums.InventionIcon;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventorsPairBonusTest {

    @Test
    void applyEffectAddsFoodWhenANewPairIsCompleted() {
        Player player = new Player("p1");
        player.getTribe().addInventor(new InventorCard(1, Era.ERA_I, 2, InventionIcon.ICON_1, "test/front.png" , "test/front.png"));
        player.getTribe().addInventor(new InventorCard(2, Era.ERA_I, 2, InventionIcon.ICON_1, "test/front.png" , "test/front.png"));

        InventorsPairBonus effect = new InventorsPairBonus();
        effect.registerSelf(player);

        player.getTribe().addInventor(new InventorCard(3, Era.ERA_I, 2, InventionIcon.ICON_2, "test/front.png" , "test/front.png"));
        effect.applyEffect(player);
        assertEquals(0, player.getFood());

        player.getTribe().addInventor(new InventorCard(4, Era.ERA_I, 2, InventionIcon.ICON_2, "test/front.png" , "test/front.png"));
        effect.applyEffect(player);
        assertEquals(3, player.getFood());

        effect.applyEffect(player);
        assertEquals(3, player.getFood());
    }

    @Test
    void applyEffectRewardsAtMostOnePairPerInvocation() {
        Player player = new Player("p1");
        InventorsPairBonus effect = new InventorsPairBonus();
        effect.registerSelf(player);

        player.getTribe().addInventor(new InventorCard(10, Era.ERA_I, 2, InventionIcon.ICON_2, "test/front.png" , "test/front.png"));
        player.getTribe().addInventor(new InventorCard(11, Era.ERA_I, 2, InventionIcon.ICON_2 ,"test/front.png" , "test/front.png"));
        player.getTribe().addInventor(new InventorCard(12, Era.ERA_I, 2, InventionIcon.ICON_3, "test/front.png" , "test/front.png"));
        player.getTribe().addInventor(new InventorCard(13, Era.ERA_I, 2, InventionIcon.ICON_3, "test/front.png" , "test/front.png"));

        effect.applyEffect(player);
        assertEquals(3, player.getFood());

        effect.applyEffect(player);
        assertEquals(6, player.getFood());
    }
}

