package model.board;

import model.enums.TotemLocation;
import model.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TurnOrderTile Tests")
class TurnOrderTileTest {

    @Test
    @DisplayName("returnTotemAndResolveEffects places on first free slot and applies bonus")
    void returnTotemUsesFirstFreeSlotAndAppliesBonus() {
        TurnOrderTile tile = new TurnOrderTile();
        TurnOrderSlot first = new TurnOrderSlot(2, false);
        TurnOrderSlot second = new TurnOrderSlot(0, true);
        tile.setup(List.of(first, second), "image.png");

        Player player = new Player("Player");
        tile.returnTotemAndResolveEffects(player);

        assertAll(
                () -> assertEquals(player, first.getOccupant()),
                () -> assertTrue(second.isFree()),
                () -> assertEquals(2, player.getFood()),
                () -> assertEquals(TotemLocation.TURN_ORDER_TILE, player.getLocation())
        );
    }

    @Test
    @DisplayName("returnTotemAndResolveEffects can apply last-slot malus")
    void returnTotemAppliesLastSlotMalusWhenFirstSlotsOccupied() {
        TurnOrderTile tile = new TurnOrderTile();
        TurnOrderSlot first = new TurnOrderSlot(0, false);
        TurnOrderSlot last = new TurnOrderSlot(0, true);
        tile.setup(List.of(first, last), "image.png");

        first.placeTotem(new Player("AlreadyPlaced"));

        Player player = new Player("Player");
        player.addPrestigePoints(5);

        tile.returnTotemAndResolveEffects(player);

        assertAll(
                () -> assertEquals(player, last.getOccupant()),
                () -> assertEquals(0, player.getFood()),
                () -> assertEquals(3, player.getPrestigePoints(), "Last slot charges 1 food or 2 prestige")
        );
    }

    @Test
    @DisplayName("returnTotemAndResolveEffects throws when all slots are occupied")
    void returnTotemThrowsWhenNoFreeSlots() {
        TurnOrderTile tile = new TurnOrderTile();
        TurnOrderSlot first = new TurnOrderSlot(0, false);
        TurnOrderSlot second = new TurnOrderSlot(0, true);
        tile.setup(List.of(first, second), "image.png");

        first.placeTotem(new Player("P1"));
        second.placeTotem(new Player("P2"));

        assertThrows(NoSuchElementException.class, () -> tile.returnTotemAndResolveEffects(new Player("P3")));
    }

    @Test
    @DisplayName("getTurnOrder returns only occupied players in slot order")
    void getTurnOrderReturnsOccupiedPlayersInOrder() {
        TurnOrderTile tile = new TurnOrderTile();
        TurnOrderSlot first = new TurnOrderSlot(0, false);
        TurnOrderSlot second = new TurnOrderSlot(0, false);
        TurnOrderSlot third = new TurnOrderSlot(0, true);
        tile.setup(List.of(first, second, third), "image.png");

        Player p1 = new Player("P1");
        Player p3 = new Player("P3");
        first.placeTotem(p1);
        third.placeTotem(p3);

        assertEquals(List.of(p1, p3), tile.getTurnOrder());
    }

}

