package model.board.OfferTileAction;

import model.GameModel;
import model.cards.Card;
import model.player.Player;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DrawCardsAction Tests")
class DrawCardsActionTest {

    @Test
    @DisplayName("canDraw and performDraw respect top and bottom row limits")
    void canDrawRespectsConfiguredLimits() {
        DrawCardsAction action = new DrawCardsAction(1, 1);
        RowsManager rowsManager = mock(RowsManager.class);

        Card topCard = mock(Card.class);
        when(topCard.getId()).thenReturn(10);
        when(rowsManager.topRowContainsCard(10)).thenReturn(true);

        Card bottomCard = mock(Card.class);
        when(bottomCard.getId()).thenReturn(20);
        when(rowsManager.bottomRowContainsCard(20)).thenReturn(true);

        action.onEnterAction(new Player("Player"), mock(GameModel.class));

        assertTrue(action.canDraw(topCard, rowsManager));
        action.performDraw(topCard, rowsManager);
        assertFalse(action.canDraw(topCard, rowsManager), "Top-row quota is exhausted");

        assertTrue(action.canDraw(bottomCard, rowsManager));
        action.performDraw(bottomCard, rowsManager);
        assertFalse(action.canDraw(bottomCard, rowsManager), "Bottom-row quota is exhausted");

        assertTrue(action.isFinished(), "Action finishes when both quotas are exhausted");
    }

    @Test
    @DisplayName("onEnterAction resets usage counters between turns")
    void onEnterActionResetsCounters() {
        DrawCardsAction action = new DrawCardsAction(1, 0);
        RowsManager rowsManager = mock(RowsManager.class);

        Card topCard = mock(Card.class);
        when(topCard.getId()).thenReturn(30);
        when(rowsManager.topRowContainsCard(30)).thenReturn(true);

        action.onEnterAction(new Player("P1"), mock(GameModel.class));
        action.performDraw(topCard, rowsManager);
        assertFalse(action.canDraw(topCard, rowsManager));

        action.onEnterAction(new Player("P2"), mock(GameModel.class));
        assertTrue(action.canDraw(topCard, rowsManager), "A new action entry must reset counters");
    }

    @Test
    @DisplayName("cards not present on rows are never drawable")
    void canDrawReturnsFalseForCardOutsideRows() {
        DrawCardsAction action = new DrawCardsAction(2, 2);
        RowsManager rowsManager = mock(RowsManager.class);

        Card unknownCard = mock(Card.class);
        when(unknownCard.getId()).thenReturn(99);
        when(rowsManager.topRowContainsCard(99)).thenReturn(false);
        when(rowsManager.bottomRowContainsCard(99)).thenReturn(false);

        action.onEnterAction(new Player("Player"), mock(GameModel.class));

        assertFalse(action.canDraw(unknownCard, rowsManager));
    }
}

