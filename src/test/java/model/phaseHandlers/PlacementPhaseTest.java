package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.board.OfferTile;
import model.enums.TotemLocation;
import model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.PlaceTotemCommand;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PlacementPhase Tests")
class PlacementPhaseTest {

    private GameModel model;
    private Board board;
    private OfferTile tileA;
    private OfferTile tileB;
    private OfferTile tileC;

    private Player p1;
    private Player p2;
    private Player p3;

    private PlacementPhase phase;

    @BeforeEach
    void setUp() {
        model = mock(GameModel.class);
        board = mock(Board.class);
        tileA = mock(OfferTile.class);
        tileB = mock(OfferTile.class);
        tileC = mock(OfferTile.class);

        p1 = mock(Player.class);
        p2 = mock(Player.class);
        p3 = mock(Player.class);

        when(model.getBoard()).thenReturn(board);
        when(model.getTurnOrder()).thenReturn(List.of(p1, p2, p3));

        when(p1.getName()).thenReturn("Player1");
        when(p2.getName()).thenReturn("Player2");
        when(p3.getName()).thenReturn("Player3");

        when(p1.getLocation()).thenReturn(TotemLocation.TURN_ORDER_TILE);
        when(p2.getLocation()).thenReturn(TotemLocation.TURN_ORDER_TILE);
        when(p3.getLocation()).thenReturn(TotemLocation.TURN_ORDER_TILE);

        when(board.findTileByLetter('A')).thenReturn(tileA);
        when(board.findTileByLetter('B')).thenReturn(tileB);
        when(board.findTileByLetter('C')).thenReturn(tileC);
        when(tileA.isOccupied()).thenReturn(false);
        when(tileB.isOccupied()).thenReturn(false);
        when(tileC.isOccupied()).thenReturn(false);

        when(model.getPlayerByName("Player1")).thenReturn(p1);
        when(model.getPlayerByName("Player2")).thenReturn(p2);
        when(model.getPlayerByName("Player3")).thenReturn(p3);

        when(tileA.getLetter()).thenReturn('A');
        when(tileB.getLetter()).thenReturn('B');
        when(tileC.getLetter()).thenReturn('C');

        when(p1.isConnected()).thenReturn(true);
        when(p2.isConnected()).thenReturn(true);
        when(p3.isConnected()).thenReturn(true);

        phase = new PlacementPhase(model);
    }

    @Test
    @DisplayName("onEnter should set first player and notify placement start")
    void onEnterSetsCurrentPlayerAndNotifies() {
        phase.onEnter();

        assertEquals(p1, phase.getCurrentPlayer());
        verify(model, times(1)).notifyChange();
    }

    @Test
    @DisplayName("visit(PlaceTotemCommand) should place totem for current player and advance turn")
    void placeTotemValidMovePlacesAndAdvances() throws Exception {
        phase.onEnter();

        phase.visit(new PlaceTotemCommand("Player1", 'A'));

        verify(board, times(1)).findTileByLetter('A');
        verify(board, times(1)).placeTotem(p1, tileA);
        assertEquals(p2, phase.getCurrentPlayer());
    }

    @Test
    @DisplayName("visit(PlaceTotemCommand) should throw when tile is already occupied")
    void placeTotemOccupiedTileThrows() throws Exception {
        phase.onEnter();
        when(tileA.isOccupied()).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> phase.visit(new PlaceTotemCommand("Player1", 'A')));
        verify(board, never()).placeTotem(any(Player.class), any(OfferTile.class));
    }

    @Test
    @DisplayName("visit(PlaceTotemCommand) should throw when tile letter does not exist")
    void placeTotemUnknownTileThrows() throws Exception {
        phase.onEnter();
        when(board.findTileByLetter('Z')).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> phase.visit(new PlaceTotemCommand("Player1", 'Z')));
        verify(board, never()).placeTotem(any(Player.class), any(OfferTile.class));
    }

    @Test
    @DisplayName("visit(PlaceTotemCommand) should transition to ActionPhase after last player places")
    void placeTotemLastPlayerTransitionsToActionPhase() throws Exception {
        phase.onEnter();

        phase.visit(new PlaceTotemCommand("Player1", 'A'));
        phase.visit(new PlaceTotemCommand("Player2", 'B'));
        phase.visit(new PlaceTotemCommand("Player3", 'C'));

        verify(model, times(4)).notifyChange();
        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof ActionPhase));
    }


    @Test
    @DisplayName("placeTotem throws when player's totem is not on the turn order tile")
    void placeTotemThrowsWhenPlayerNotOnTurnOrderTile() {
        when(p1.getLocation()).thenReturn(TotemLocation.OFFER_TRACK);
        phase.onEnter();

        assertThrows(IllegalArgumentException.class,
                () -> phase.visit(new PlaceTotemCommand("Player1", 'A')));
        verify(board, never()).placeTotem(any(Player.class), any(OfferTile.class));
    }

    @Test
    @DisplayName("skipCurrentPlayerTurn advances to the next player in turn order")
    void skipCurrentPlayerTurnAdvancesToNextPlayer() {
        phase.onEnter();
        assertEquals(p1, phase.getCurrentPlayer());

        phase.skipCurrentPlayerTurn();

        assertEquals(p2, phase.getCurrentPlayer());
        verify(model, times(2)).notifyChange();
    }

    @Test
    @DisplayName("EndTurnCommand throws during PlacementPhase")
    void endTurnThrows() {
        assertThrows(IllegalStateException.class,
                () -> phase.visit(new EndTurnCommand("Player1")));
    }

    @Test
    @DisplayName("DrawCardCommand throws during PlacementPhase")
    void drawCardThrows() {
        assertThrows(IllegalStateException.class,
                () -> phase.visit(new DrawCardCommand("Player1", 45)));
    }


}
