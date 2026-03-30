package model.buildingEffects.OnCharacterAcquiredEffects;

import model.cards.charachterCards.ArtistCard;
import model.cards.charachterCards.BuilderCard;
import model.cards.charachterCards.GathererCard;
import model.cards.charachterCards.HunterCard;
import model.cards.charachterCards.InventorCard;
import model.cards.charachterCards.ShamanCard;
import model.enums.Era;
import model.enums.InventionIcon;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BonusForCompletedSetTest {

    @Test
    void applyEffectAddsFoodOnlyWhenCompletedSetsIncrease() {
        Player player = new Player("p1");
        addCompleteSet(player, 0);

        BonusForCompletedSet effect = new BonusForCompletedSet();
        effect.registerSelf(player);

        effect.applyEffect(player);
        assertEquals(0, player.getFood()); //note that this effect gives food only for the completed set after the acquisition of the building card

        addCompleteSet(player, 20);
        effect.applyEffect(player);
        assertEquals(5, player.getFood());

        effect.applyEffect(player);
        assertEquals(5, player.getFood());

        addCompleteSet(player, 30);
        effect.applyEffect(player);
        assertEquals(10, player.getFood());
    }

    private void addCompleteSet(Player player, int baseId) {
        player.getTribe().addArtist(new ArtistCard(baseId + 1, Era.ERA_I, 2));
        player.getTribe().addBuilder(new BuilderCard(baseId + 2, Era.ERA_I, 2, 1, 1));
        player.getTribe().addGatherer(new GathererCard(baseId + 3, Era.ERA_I, 2));
        player.getTribe().addHunter(new HunterCard(baseId + 4, Era.ERA_I, 2, false));
        player.getTribe().addInventor(new InventorCard(baseId + 5, Era.ERA_I, 2, InventionIcon.ICON_1));
        player.getTribe().addShaman(new ShamanCard(baseId + 6, Era.ERA_I, 2, 1));
    }
}

