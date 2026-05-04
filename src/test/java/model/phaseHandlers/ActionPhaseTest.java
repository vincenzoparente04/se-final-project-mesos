package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.board.OfferTile;
import model.board.OfferTileAction.OfferTileAction;
import model.board.OfferTrack;
import model.board.TurnOrderTile;
import model.cards.Card;
import model.cards.buildingCards.BuildingCard;
import model.cards.characterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.player.Player;
import model.player.Tribe;
import model.rowsManager.CardVisitor;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ActionPhase Tests")
class ActionPhaseTest {

    private GameModel model;
    private Board board;
    private OfferTrack offerTrack;
    private OfferTile occupiedTile;
    private OfferTileAction action;
    private RowsManager rowsManager;
    private TurnOrderTile turnOrderTile;

    private Player p1;
    private Tribe t1;

    private ActionPhase phase;

    @BeforeEach
    void setUp() {
        model = mock(GameModel.class);
        board = mock(Board.class);
        offerTrack = mock(OfferTrack.class);
        occupiedTile = mock(OfferTile.class);
        action = mock(OfferTileAction.class);
        rowsManager = mock(RowsManager.class);
        turnOrderTile = mock(TurnOrderTile.class);

        p1 = mock(Player.class);
        t1 = mock(Tribe.class);
        when(p1.getName()).thenReturn("Player1");
        when(p1.getTribe()).thenReturn(t1);

        when(model.getBoard()).thenReturn(board);
        when(model.getRowsManager()).thenReturn(rowsManager);
        when(board.getOfferTrack()).thenReturn(offerTrack);
        when(board.getTurnOrderTile()).thenReturn(turnOrderTile);

        phase = new ActionPhase(model);
    }

    @Test
    @DisplayName("onEnter should transition to PreEndOfRoundPhase when no player is on offer track")
    void onEnterNoPlayerTransitionsToPreEndOfRound() {
        when(board.getNextPlayerOnOfferTrack()).thenReturn(null);

        phase.onEnter();

        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof PreEndOfRoundPhase));
    }

    @Test
    @DisplayName("onEnter should start active player turn and initialize action")
    void onEnterStartsTurnAndInitializesAction() {
        CharacterCard legalCard = mock(CharacterCard.class);
        when(legalCard.getId()).thenReturn(1);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(legalCard);
            return null;
        }).when(legalCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(legalCard));
        when(action.canDraw(legalCard, rowsManager)).thenReturn(true);

        phase.onEnter();

        assertEquals(p1, phase.getCurrentPlayer());
        verify(model, times(1)).notifyChange();
        verify(action, times(1)).onEnterAction(p1, model);
        verify(turnOrderTile, never()).returnTotemAndResolveEffects(p1);
    }

    @Test
    @DisplayName("drawCard should throw when no action turn is active")
    void drawCardWithoutActiveTurnThrows() {
        assertThrows(IllegalStateException.class, () -> phase.drawCard(10));
    }

    @Test
    @DisplayName("drawCard should execute card acquisition flow for a legal draw")
    void drawCardLegalFlowExecutesSuccessfully() {
        CharacterCard legalCard = mock(CharacterCard.class);
        CharacterCard selectedCard = mock(CharacterCard.class);

        when(legalCard.getId()).thenReturn(1);
        when(selectedCard.getId()).thenReturn(10);

        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(legalCard);
            return null;
        }).when(legalCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(selectedCard);
            return null;
        }).when(selectedCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        // Keep the turn active both after onEnter and after drawCard checks.
        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(legalCard));
        when(action.canDraw(legalCard, rowsManager)).thenReturn(true);

        when(rowsManager.findCardById(10)).thenReturn(Optional.of(selectedCard));
        when(action.canDraw(selectedCard, rowsManager)).thenReturn(true);

        phase.onEnter();
        phase.drawCard(10);

        verify(rowsManager, times(1)).removeCard(10);
        verify(selectedCard, times(1)).registerToTribe(p1);
        verify(action, times(1)).performDraw(selectedCard, rowsManager);
        verify(model, times(2)).notifyChange();
    }

    @Test
    @DisplayName("drawCard should throw when card is not found")
    void drawCardMissingCardThrows() {
        CharacterCard legalCard = mock(CharacterCard.class);
        when(legalCard.getId()).thenReturn(1);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(legalCard);
            return null;
        }).when(legalCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(legalCard));
        when(action.canDraw(legalCard, rowsManager)).thenReturn(true);

        when(rowsManager.findCardById(999)).thenReturn(Optional.empty());

        phase.onEnter();

        assertThrows(IllegalArgumentException.class, () -> phase.drawCard(999));
    }

    @Test
    @DisplayName("drawCard should throw when selected card cannot be drawn by current action")
    void drawCardCannotDrawThrows() {
        Card selectedCard = mock(Card.class);
        CharacterCard legalCard = mock(CharacterCard.class);
        when(legalCard.getId()).thenReturn(1);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(legalCard);
            return null;
        }).when(legalCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(legalCard));
        when(action.canDraw(legalCard, rowsManager)).thenReturn(true);

        when(rowsManager.findCardById(50)).thenReturn(Optional.of(selectedCard));
        when(action.canDraw(selectedCard, rowsManager)).thenReturn(false);

        phase.onEnter();

        assertThrows(IllegalStateException.class, () -> phase.drawCard(50));
        verify(rowsManager, never()).removeCard(50);
        verify(action, never()).performDraw(selectedCard, rowsManager);
    }

    @Test
    @DisplayName("drawCard should throw when selected card is an event")
    void drawCardEventCardThrows() {
        EventCard selectedCard = mock(EventCard.class);
        CharacterCard legalCard = mock(CharacterCard.class);

        when(legalCard.getId()).thenReturn(1);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(legalCard);
            return null;
        }).when(legalCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        when(selectedCard.getId()).thenReturn(60);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(selectedCard);
            return null;
        }).when(selectedCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(legalCard));
        when(action.canDraw(legalCard, rowsManager)).thenReturn(true);

        when(rowsManager.findCardById(60)).thenReturn(Optional.of(selectedCard));
        when(action.canDraw(selectedCard, rowsManager)).thenReturn(true);

        phase.onEnter();

        assertThrows(IllegalStateException.class, () -> phase.drawCard(60));
        verify(rowsManager, never()).removeCard(60);
        verify(selectedCard, never()).registerToTribe(p1);
    }

    @Test
    @DisplayName("endTurn should throw when no active turn")
    void endTurnWithoutActiveTurnThrows() {
        assertThrows(IllegalStateException.class, () -> phase.endTurn());
    }

    @Test
    @DisplayName("endTurn should throw when there are forced moves remaining")
    void endTurnWithForcedMovesThrows() {
        CharacterCard forcedCard = mock(CharacterCard.class);
        when(forcedCard.getId()).thenReturn(1);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(forcedCard);
            return null;
        }).when(forcedCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);
        when(action.isFinished()).thenReturn(false);
        when(action.canDraw(forcedCard, rowsManager)).thenReturn(true);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(forcedCard));
        when(rowsManager.getAllTribeCardsOnBoard()).thenReturn(List.of(forcedCard));

        phase.onEnter();

        assertThrows(IllegalStateException.class, () -> phase.endTurn());
    }

    @Test
    @DisplayName("endTurn with no forced moves notifies, returns totem and advances to next player")
    void endTurnWithNoForcedMovesAdvancesToNextPlayer() {
        BuildingCard buildingCard = mock(BuildingCard.class);
        when(buildingCard.getId()).thenReturn(5);
        when(buildingCard.getDiscountedCost(p1)).thenReturn(2);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(buildingCard);
            return null;
        }).when(buildingCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1).thenReturn(null);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);
        when(action.isFinished()).thenReturn(false);
        when(action.canDraw(buildingCard, rowsManager)).thenReturn(true);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(buildingCard));
        when(rowsManager.getAllTribeCardsOnBoard()).thenReturn(List.of()); // no forced moves
        when(p1.getFood()).thenReturn(10);

        phase.onEnter();
        phase.endTurn();

        verify(model, times(2)).notifyChange();
        verify(board).returnTotemToTurnOrder(p1);
    }

    @Test
    @DisplayName("onEnter should auto-advance when action is not finished but no legal move exists")
    void onEnterNoLegalMoveAutoAdvances() {
        EventCard blockedCard = mock(EventCard.class);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(blockedCard);
            return null;
        }).when(blockedCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1).thenReturn(null);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(blockedCard));
        when(action.canDraw(blockedCard, rowsManager)).thenReturn(true);

        phase.onEnter();

        verify(board, times(1)).returnTotemToTurnOrder(p1);
        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof PreEndOfRoundPhase));
    }

    @Test
    @DisplayName("onEnter should auto-advance and end action phase when current action is finished")
    void onEnterFinishedActionAutoAdvancesAndEndsPhase() {
        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1).thenReturn(null);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);
        when(action.isFinished()).thenReturn(true);

        phase.onEnter();

        verify(board, times(1)).returnTotemToTurnOrder(p1);
        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof PreEndOfRoundPhase));
    }
}


