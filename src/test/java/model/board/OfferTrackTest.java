package model.board;

import model.board.OfferTileAction.TakeFoodAction;
import model.enums.TotemLocation;
import model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("OfferTrack Tests")
class OfferTrackTest {

    private OfferTrack offerTrack;
    private OfferTile tileB;
    private OfferTile tileC;
    private OfferTile tileD;

    @BeforeEach
    void setUp() {
        offerTrack = new OfferTrack(); //all tiles takeFoodAction for testing purposes
        tileB = new OfferTile('B', new TakeFoodAction(1), "B_front.png", "B_back.png");
        tileC = new OfferTile('C', new TakeFoodAction(2), "C_front.png", "C_back.png");
        tileD = new OfferTile('D', new TakeFoodAction(3), "D_front.png", "D_back.png");
        offerTrack.setup(List.of(tileB, tileC, tileD));
    }

    @Test
    @DisplayName("placeTotem occupies tile and moves player to offer track")
    void placeTotemOccupiesTileAndUpdatesPlayerLocation() {
        Player player = new Player("Player");

        offerTrack.placeTotem(player, tileC);

        assertAll(
                () -> assertEquals(player, tileC.getOccupant()),
                () -> assertEquals(TotemLocation.OFFER_TRACK, player.getLocation())
        );
    }

    @Test
    @DisplayName("getTileByLetter returns matching tile or null")
    void getTileByLetterReturnsExpectedTile() {
        assertAll(
                () -> assertEquals(tileB, offerTrack.getTileByLetter('B')),
                () -> assertEquals(tileC, offerTrack.getTileByLetter('C')),
                () -> assertNull(offerTrack.getTileByLetter('A'))
        );
    }

    @Test
    @DisplayName("getNextPlayer returns occupant of first occupied tile in track order")
    void getNextPlayerUsesTrackOrder() {
        Player playerOnD = new Player("D-player");
        Player playerOnC = new Player("C-player");

        offerTrack.placeTotem(playerOnD, tileD);
        offerTrack.placeTotem(playerOnC, tileC);

        assertEquals(playerOnC, offerTrack.getNextPlayer());
    }

    @Test
    @DisplayName("getOccupiedTileByPlayer finds the exact occupied tile")
    void getOccupiedTileByPlayerReturnsPlayersTile() {
        Player player = new Player("Player");
        Player other = new Player("Other");
        offerTrack.placeTotem(player, tileB);

        assertAll(
                () -> assertEquals(tileB, offerTrack.getOccupiedTileByPlayer(player)),
                () -> assertNull(offerTrack.getOccupiedTileByPlayer(other))
        );
    }
}

