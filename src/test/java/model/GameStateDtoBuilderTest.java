package model;

import model.board.Board;
import model.board.OfferTile;
import model.board.OfferTileAction.TakeFoodAction;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shared.dto.GameStateDto;
import shared.dto.OfferTileDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameStateDtoBuilderTest {

    @Test
    @DisplayName("visitTakeFood should correctly set TAKE_FOOD label and null limits")
    void testVisitTakeFoodInActionDetailsExtractor() {
        // Arrange
        GameModel mockModel = mock(GameModel.class);
        Board mockBoard = mock(Board.class);
        RowsManager mockRowsManager = mock(RowsManager.class);
        TakeFoodAction takeFoodAction = mock(TakeFoodAction.class);
        OfferTile mockTile = mock(OfferTile.class);
        
        when(mockModel.getPlayers()).thenReturn(List.of());
        when(mockModel.getBoard()).thenReturn(mockBoard);
        when(mockModel.getRowsManager()).thenReturn(mockRowsManager);
        
        when(mockBoard.getOfferTiles()).thenReturn(List.of(mockTile));
        when(mockBoard.getTurnOrderSlots()).thenReturn(List.of());
        
        when(mockTile.getLetter()).thenReturn('A');
        when(mockTile.isOccupied()).thenReturn(false);
        when(mockTile.getAction()).thenReturn(takeFoodAction);
        
        doAnswer(invocation -> {
            model.board.OfferTileAction.OfferTileActionVisitor visitor = invocation.getArgument(0);
            visitor.visitTakeFood(takeFoodAction);
            return null;
        }).when(takeFoodAction).accept(any());

        // Act
        GameStateDto dto = GameStateDtoBuilder.build(mockModel);

        // Assert
        assertNotNull(dto);
        List<OfferTileDto> tiles = dto.offerTiles;
        assertEquals(1, tiles.size());
        
        OfferTileDto tileDto = tiles.get(0);
        assertEquals('A', tileDto.letter);
        assertEquals("TAKE_FOOD", tileDto.actionType);
        assertNull(tileDto.topRowLimit);
        assertNull(tileDto.bottomRowLimit);
        assertNull(tileDto.topRowUsed);
        assertNull(tileDto.bottomRowUsed);
    }
}
