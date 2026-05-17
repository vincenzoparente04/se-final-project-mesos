package model;

import model.board.Board;
import model.board.OfferTile;
import model.board.TurnOrderSlot;
import model.board.OfferTileAction.DrawCardsAction;
import model.board.OfferTileAction.OfferTileAction;
import model.board.OfferTileAction.OfferTileActionVisitor;
import model.board.OfferTileAction.TakeFoodAction;
import model.enums.Era;
import model.enums.GamePhase;
import model.player.Player;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shared.dto.GameStateDto;
import shared.dto.OfferTileDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GameStateDtoBuilder Tests")
public class GameStateDtoBuilderTest {

    private GameModel model;
    private Board board;
    private RowsManager rowsManager;

    @BeforeEach
    void setUp() {
        model = mock(GameModel.class);
        board = mock(Board.class);
        rowsManager = mock(RowsManager.class);

        when(model.getPlayers()).thenReturn(List.of());
        when(model.getBoard()).thenReturn(board);
        when(model.getRowsManager()).thenReturn(rowsManager);
        when(model.getWinners()).thenReturn(List.of());

        when(board.getOfferTiles()).thenReturn(List.of());
        when(board.getTurnOrderSlots()).thenReturn(List.of());

        when(rowsManager.getTopRowTribe()).thenReturn(List.of());
        when(rowsManager.getBottomRowTribe()).thenReturn(List.of());
        when(rowsManager.getTopRowBuilding()).thenReturn(List.of());
        when(rowsManager.getBottomRowBuilding()).thenReturn(List.of());
    }

    // ── ActionDetailsExtractor ─────────────────────────────────────────────────

    @Test
    @DisplayName("visitTakeFood sets TAKE_FOOD label and null row limits")
    void visitTakeFoodSetsLabelAndNullLimits() {
        TakeFoodAction takeFoodAction = mock(TakeFoodAction.class);
        doAnswer(inv -> { inv.<OfferTileActionVisitor>getArgument(0).visitTakeFood(takeFoodAction); return null; })
                .when(takeFoodAction).accept(any());
        OfferTile tile = mockTile('A', takeFoodAction);
        when(board.getOfferTiles()).thenReturn(List.of(tile));

        OfferTileDto tileDto = GameStateDtoBuilder.build(model).offerTiles.get(0);

        assertEquals('A', tileDto.letter);
        assertEquals("TAKE_FOOD", tileDto.actionType);
        assertNull(tileDto.topRowLimit);
        assertNull(tileDto.bottomRowLimit);
        assertNull(tileDto.topRowUsed);
        assertNull(tileDto.bottomRowUsed);
    }

    @Test
    @DisplayName("visitDrawCards sets DRAW_CARDS label and populates row limits and current counts")
    void visitDrawCardsSetsLabelAndRowLimits() {
        DrawCardsAction drawAction = mock(DrawCardsAction.class);
        when(drawAction.getMaxTopRowDraws()).thenReturn(2);
        when(drawAction.getMaxBottomRowDraws()).thenReturn(1);
        when(drawAction.getCurrentTopRowDraws()).thenReturn(1);
        when(drawAction.getCurrentBottomRowDraws()).thenReturn(0);
        doAnswer(inv -> { inv.<OfferTileActionVisitor>getArgument(0).visitDrawCards(drawAction); return null; })
                .when(drawAction).accept(any());
        OfferTile tile = mockTile('B', drawAction);
        when(board.getOfferTiles()).thenReturn(List.of(tile));

        OfferTileDto tileDto = GameStateDtoBuilder.build(model).offerTiles.get(0);

        assertEquals("DRAW_CARDS", tileDto.actionType);
        assertEquals(2, tileDto.topRowLimit);
        assertEquals(1, tileDto.bottomRowLimit);
        assertEquals(1, tileDto.topRowUsed);
        assertEquals(0, tileDto.bottomRowUsed);
    }

    // ── build() null-state branches ───────────────────────────────────────────

    @Test
    @DisplayName("build with null phase, null currentPlayer, and null era produces null fields in dto")
    void buildWithNullPhasePlayerAndEraProducesNullFields() {
        when(model.getCurrentPhase()).thenReturn(null);
        when(model.getCurrentPlayer()).thenReturn(null);
        when(model.getCurrentEra()).thenReturn(null);

        GameStateDto dto = GameStateDtoBuilder.build(model);

        assertNull(dto.phase);
        assertNull(dto.currentPlayerName);
        assertNull(dto.currentEra);
    }

    @Test
    @DisplayName("build with non-null phase, player, and era populates those fields correctly")
    void buildWithNonNullPhasePlayerAndEraPopulatesFields() {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alice");
        when(model.getCurrentPhase()).thenReturn(GamePhase.ACTION);
        when(model.getCurrentPlayer()).thenReturn(player);
        when(model.getCurrentEra()).thenReturn(Era.ERA_II);

        GameStateDto dto = GameStateDtoBuilder.build(model);

        assertEquals("ACTION", dto.phase);
        assertEquals("Alice", dto.currentPlayerName);
        // The builder emits the 1-based era index as a string, not the enum name.
        assertEquals("2", dto.currentEra);
    }

    // ── winners branch ────────────────────────────────────────────────────────

    @Test
    @DisplayName("build maps non-empty winners list directly into dto")
    void buildWithNonEmptyWinnersPopulatesWinnersList() {
        when(model.getWinners()).thenReturn(List.of("Alice", "Bob"));

        GameStateDto dto = GameStateDtoBuilder.build(model);

        assertNotNull(dto.winners);
        assertEquals(List.of("Alice", "Bob"), dto.winners);
    }

    @Test
    @DisplayName("build maps empty winners list to null in dto")
    void buildWithEmptyWinnersMapsToNull() {
        when(model.getWinners()).thenReturn(List.of());

        assertNull(GameStateDtoBuilder.build(model).winners);
    }

    // ── TurnOrderSlot mapping ─────────────────────────────────────────────────

    @Test
    @DisplayName("build maps occupied turn-order slot to occupant name with correct position index")
    void buildMapsOccupiedTurnOrderSlotToOccupantName() {
        TurnOrderSlot slot = mock(TurnOrderSlot.class);
        Player occupant = mock(Player.class);
        when(slot.isOccupied()).thenReturn(true);
        when(slot.getOccupant()).thenReturn(occupant);
        when(occupant.getName()).thenReturn("Alice");
        when(board.getTurnOrderSlots()).thenReturn(List.of(slot));

        GameStateDto dto = GameStateDtoBuilder.build(model);

        assertEquals(0, dto.turnOrderSlots.get(0).position);
        assertEquals("Alice", dto.turnOrderSlots.get(0).occupantName);
    }

    @Test
    @DisplayName("build maps empty turn-order slot to null occupant name")
    void buildMapsEmptyTurnOrderSlotToNullOccupantName() {
        TurnOrderSlot slot = mock(TurnOrderSlot.class);
        when(slot.isOccupied()).thenReturn(false);
        when(board.getTurnOrderSlots()).thenReturn(List.of(slot));

        assertNull(GameStateDtoBuilder.build(model).turnOrderSlots.get(0).occupantName);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static OfferTile mockTile(char letter, OfferTileAction action) {
        OfferTile tile = mock(OfferTile.class);
        when(tile.getLetter()).thenReturn(letter);
        when(tile.isOccupied()).thenReturn(false);
        when(tile.getAction()).thenReturn(action);
        return tile;
    }
}
