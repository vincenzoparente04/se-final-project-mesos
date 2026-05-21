package model.board;

import model.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("TurnOrderSlot Tests")
class TurnOrderSlotTest {

    @Test
    @DisplayName("applyEffect - gives only food bonus when slot is not last")
    void applyEffectAddsOnlyFoodBonusWhenNotLast() {
        //Arrange
        TurnOrderSlot slot = new TurnOrderSlot(3, false);
        Player player = new Player("Player");

        //Act
        slot.placeTotem(player);
        slot.applyEffect();

        //Assert
        assertAll(
            () -> assertEquals(3, player.getFood(), "Food bonus should be added"),
            () -> assertEquals(0, player.getPrestigePoints(), "No prestige points should be removed")
        );
    }

    @Test
    @DisplayName("applyEffect - applies only last-slot malus when bonus is zero")
    void applyEffectAppliesOnlyLastMalusWhenNoBonus() {
        //Arrange
        TurnOrderSlot slot = new TurnOrderSlot(0, true);
        Player player = new Player("Player");
        player.addFood(1);

        //Act
        slot.placeTotem(player);
        slot.applyEffect();

        //Assert
        assertAll(
                () -> assertEquals(0, player.getFood(), "One food should be removed"),
                () -> assertEquals(0, player.getPrestigePoints(), "No prestige points should be removed when food is sufficent")
        );
    }


    @Test
    @DisplayName("applyEffect - spends two prestige points when last-slot malus cannot be paid with food")
    void applyEffectConvertsMissingFoodIntoPrestigeCost() {
        TurnOrderSlot slot = new TurnOrderSlot(0, true);
        Player player = new Player("Player");
        player.addPrestigePoints(6);
        slot.placeTotem(player);

        slot.applyEffect();

        assertAll(
                () -> assertEquals(0, player.getFood(), "Food remains zero"),
                () -> assertEquals(4, player.getPrestigePoints(), "Missing food costs 2 prestige points")
        );
    }

    @Test
    @DisplayName("applyEffect - throws when called on a free slot")
    void applyEffectThrowsWhenNoOccupant() {
        TurnOrderSlot slot = new TurnOrderSlot(1, false);

        assertThrows(NullPointerException.class, slot::applyEffect,
                "Current implementation requires an occupant before applying effect");
    }
}
