package model.phaseHandlers;

import model.board.OfferTileAction.OfferTileAction;
import model.cards.buildingCards.BuildingCard;
import model.cards.characterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.player.Player;
import model.rowsManager.CardVisitor;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("MoveChecker Tests")
class MoveCheckerTest {

    private Player player;
    private OfferTileAction action;
    private RowsManager rowsManager;
    private MoveChecker checker;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        action = mock(OfferTileAction.class);
        rowsManager = mock(RowsManager.class);
        checker = new MoveChecker(player, action, rowsManager);
    }

    @Test
    @DisplayName("checkForcedMoves returns true when a drawable CharacterCard exists")
    void checkForcedMovesReturnsTrueForDrawableCharacterCard() {
        CharacterCard card = mock(CharacterCard.class);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(card);
            return null;
        }).when(card).accept(any(CardVisitor.class));
        when(action.canDraw(card, rowsManager)).thenReturn(true);

        assertTrue(checker.checkForcedMoves(List.of(card)));
    }

    @Test
    @DisplayName("checkForcedMoves returns false when CharacterCard cannot be drawn by current action")
    void checkForcedMovesReturnsFalseWhenCardNotDrawable() {
        CharacterCard card = mock(CharacterCard.class);
        when(action.canDraw(card, rowsManager)).thenReturn(false);

        assertFalse(checker.checkForcedMoves(List.of(card)));
        verify(card, never()).accept(any());
    }

    @Test
    @DisplayName("checkForcedMoves returns false on empty list")
    void checkForcedMovesReturnsFalseOnEmptyList() {
        assertFalse(checker.checkForcedMoves(List.of()));
    }

    @Test
    @DisplayName("checkLegalMoves returns true when an affordable BuildingCard exists")
    void checkLegalMovesReturnsTrueForAffordableBuildingCard() {
        BuildingCard card = mock(BuildingCard.class);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(card);
            return null;
        }).when(card).accept(any(CardVisitor.class));
        when(action.canDraw(card, rowsManager)).thenReturn(true);
        when(card.getDiscountedCost(player)).thenReturn(2);
        when(player.getFood()).thenReturn(5);

        assertTrue(checker.checkLegalMoves(List.of(card)));
    }

    @Test
    @DisplayName("checkLegalMoves returns false when BuildingCard is unaffordable")
    void checkLegalMovesReturnsFalseForUnaffordableBuildingCard() {
        BuildingCard card = mock(BuildingCard.class);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(card);
            return null;
        }).when(card).accept(any(CardVisitor.class));
        when(action.canDraw(card, rowsManager)).thenReturn(true);
        when(card.getDiscountedCost(player)).thenReturn(10);
        when(player.getFood()).thenReturn(3);

        assertFalse(checker.checkLegalMoves(List.of(card)));
    }

    @Test
    @DisplayName("visit(EventCard) does not count as legal or forced move")
    void visitEventCardDoesNotCountAsMove() {
        EventCard card = mock(EventCard.class);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(card);
            return null;
        }).when(card).accept(any(CardVisitor.class));
        when(action.canDraw(card, rowsManager)).thenReturn(true);

        assertFalse(checker.checkLegalMoves(List.of(card)));
    }

    @Test
    @DisplayName("visit(SustenanceEventCard) does not count as legal or forced move")
    void visitSustenanceEventCardDoesNotCountAsMove() {
        SustenanceEventCard card = mock(SustenanceEventCard.class);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(card);
            return null;
        }).when(card).accept(any(CardVisitor.class));
        when(action.canDraw(card, rowsManager)).thenReturn(true);

        assertFalse(checker.checkLegalMoves(List.of(card)));
    }
}
