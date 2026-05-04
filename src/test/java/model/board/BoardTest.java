package model.board;

import model.enums.TotemLocation;
import model.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Board Tests")
class BoardTest {

    @Test
    @DisplayName("setup(3) loads only eligible offer tiles and a 3-slot turn order tile")
    void setupThreePlayersBuildsExpectedBoardState() {
        Board board = new Board();

        board.setup(3);

        List<TurnOrderSlot> slots = board.getTurnOrderTile().getSlots();
        assertAll(
                () -> assertNotNull(board.findTileByLetter('B')),
                () -> assertNotNull(board.findTileByLetter('C')),
                () -> assertNotNull(board.findTileByLetter('D')),
                () -> assertNotNull(board.findTileByLetter('E')),
                () -> assertNotNull(board.findTileByLetter('F')),
                () -> assertNull(board.findTileByLetter('A'), "Tile A requires 5 players"),
                () -> assertNull(board.findTileByLetter('G'), "Tile G requires 4 players"),
                () -> assertEquals(3, slots.size(), "3-player setup must have exactly 3 turn order slots"),
                () -> assertEquals(2, slots.getFirst().getFoodBonus()),
                () -> assertEquals(0, slots.get(1).getFoodBonus()),
                () -> assertEquals(0, slots.get(2).getFoodBonus()), //foodBonus for the last slot is always -1 (handled by apply()), the getFoodBonus returns 0
                () -> assertFalse(slots.getFirst().isLast()),
                () -> assertFalse(slots.get(1).isLast()),
                () -> assertTrue(slots.get(2).isLast())
        );
    }

    @Test
    @DisplayName("placeTotem delegates to offer track and updates player location")
    void placeTotemPlacesPlayerOnSelectedTile() {
        Board board = new Board();
        board.setup(4);

        Player player = new Player("Player");
        OfferTile tile = board.findTileByLetter('E');

        board.placeTotem(player, tile);

        assertAll(
                () -> assertEquals(player, tile.getOccupant()),
                () -> assertEquals(TotemLocation.OFFER_TRACK, player.getLocation())
        );
    }

    @Test
    @DisplayName("getNextPlayerOnOfferTrack returns left-most occupied tile occupant")
    void getNextPlayerOnOfferTrackReturnsFirstOccupiedTileInTrackOrder() {
        Board board = new Board();
        board.setup(5);

        Player first = new Player("First");
        Player second = new Player("Second");

        board.placeTotem(second, board.findTileByLetter('F'));
        board.placeTotem(first, board.findTileByLetter('B'));

        assertEquals(first, board.getNextPlayerOnOfferTrack(),
                "Offer track resolves from left to right, not by placement time");
    }

    @Test
    @DisplayName("setup throws when player count has no board configuration")
    void setupThrowsForUnsupportedPlayerCount() {
        Board board = new Board();

        assertThrows(IllegalArgumentException.class, () -> board.setup(1));
    }

    @Test
    @DisplayName("returnTotemToTurnOrder removes player from offer track and places them in turn order")
    void returnTotemToTurnOrderClearsOfferTrackAndJoinsTurnOrder() {
        Board board = new Board();
        board.setup(3);

        Player player = new Player("Player");
        OfferTile tileB = board.findTileByLetter('B');

        board.placeTotem(player, tileB);
        board.returnTotemToTurnOrder(player);

        assertAll(
                () -> assertNull(tileB.getOccupant(), "Tile should be freed after returning totem"),
                () -> assertTrue(board.getTurnOrder().contains(player), "Player should appear in turn order")
        );
    }
}
