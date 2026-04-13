package model.board.OfferTileAction;

import model.GameModel;
import model.cards.Card;
import model.player.Player;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("TakeFoodAction Tests")
class TakeFoodActionTest {

    @Test
    @DisplayName("onEnterAction grants food immediately and marks action as finished")
    void onEnterActionAddsFoodAndFinishes() {
        TakeFoodAction action = new TakeFoodAction(3);
        Player player = new Player("Player");

        action.onEnterAction(player, mock(GameModel.class));

        assertAll(
                () -> assertEquals(3, player.getFood()),
                () -> assertTrue(action.isFinished())
        );
    }

    @Test
    @DisplayName("draw-related methods are disabled for food-only action")
    void drawMethodsAreNotSupported() {
        TakeFoodAction action = new TakeFoodAction(1);
        Card card = mock(Card.class);
        RowsManager rowsManager = mock(RowsManager.class);

        assertAll(
                () -> assertFalse(action.canDraw(card, rowsManager)),
                () -> assertThrows(UnsupportedOperationException.class, () -> action.performDraw(card, rowsManager))
        );
    }
}

