package model.board;

import model.board.OfferTileAction.TakeFoodAction;
import model.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OfferTile Tests")
class OfferTileTest {

    @Test
    @DisplayName("new tile starts free and exposes immutable metadata")
    void newTileStartsFreeWithExpectedMetadata() {
        TakeFoodAction action = new TakeFoodAction(3);
        OfferTile tile = new OfferTile('A', action, "front.png", "back.png");

        assertAll(
                () -> assertEquals('A', tile.getLetter()),
                () -> assertSame(action, tile.getAction()),
                () -> assertFalse(tile.isOccupied()),
                () -> assertNull(tile.getOccupant())
        );
    }

    @Test
    @DisplayName("placeTotem marks tile occupied by that player")
    void placeTotemOccupiesTile() {
        OfferTile tile = new OfferTile('B', new TakeFoodAction(1), "front.png", "back.png");
        Player player = new Player("Player");

        tile.placeTotem(player);

        assertAll(
                () -> assertTrue(tile.isOccupied()),
                () -> assertEquals(player, tile.getOccupant())
        );
    }

    @Test
    @DisplayName("removeTotem frees tile and can be called repeatedly")
    void removeTotemClearsOccupant() {
        OfferTile tile = new OfferTile('C', new TakeFoodAction(1), "front.png", "back.png");
        tile.placeTotem(new Player("Player"));

        tile.removeTotem();
        tile.removeTotem();

        assertAll(
                () -> assertFalse(tile.isOccupied()),
                () -> assertNull(tile.getOccupant())
        );
    }
}

