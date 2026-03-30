package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.board.OfferTile;
import model.board.OfferTileAction.OfferTileAction;
import model.board.OfferTrack;
import model.board.TurnOrderTile;
import model.cards.Card;
import model.cards.TribeCard;
import model.player.Player;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
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
        when(p1.getName()).thenReturn("Player1");

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
        TribeCard legalCard = mock(TribeCard.class);

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(legalCard));
        when(action.canDraw(legalCard, rowsManager)).thenReturn(true);
        when(legalCard.canBeAcquiredBy(p1, model)).thenReturn(true);

        phase.onEnter();

        assertEquals(p1, phase.getCurrentPlayer());
        verify(model, times(1)).notifyChange("action_started:Player1");
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
        Card selectedCard = mock(Card.class);
        TribeCard legalCard = mock(TribeCard.class);

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        // Keep the turn active both after onEnter and after drawCard checks.
        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(legalCard));
        when(action.canDraw(legalCard, rowsManager)).thenReturn(true);
        when(legalCard.canBeAcquiredBy(p1, model)).thenReturn(true);

        when(rowsManager.findCardById(10)).thenReturn(selectedCard);
        when(action.canDraw(selectedCard, rowsManager)).thenReturn(true);
        when(selectedCard.canBeAcquiredBy(p1, model)).thenReturn(true);

        phase.onEnter();
        phase.drawCard(10);

        verify(rowsManager, times(1)).removeCard(10);
        verify(selectedCard, times(1)).acquiredBy(p1, model);
        verify(action, times(1)).performDraw(selectedCard, rowsManager);
        verify(model, times(1)).notifyChange("card_drawn:10");
    }

    @Test
    @DisplayName("drawCard should throw when card is not found")
    void drawCardMissingCardThrows() {
        TribeCard legalCard = mock(TribeCard.class);

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(legalCard));
        when(action.canDraw(legalCard, rowsManager)).thenReturn(true);
        when(legalCard.canBeAcquiredBy(p1, model)).thenReturn(true);

        when(rowsManager.findCardById(999)).thenReturn(null);

        phase.onEnter();

        assertThrows(IllegalArgumentException.class, () -> phase.drawCard(999));
    }

    @Test
    @DisplayName("drawCard should throw when selected card cannot be drawn by current action")
    void drawCardCannotDrawThrows() {
        Card selectedCard = mock(Card.class);
        TribeCard legalCard = mock(TribeCard.class);

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(legalCard));
        when(action.canDraw(legalCard, rowsManager)).thenReturn(true);
        when(legalCard.canBeAcquiredBy(p1, model)).thenReturn(true);

        when(rowsManager.findCardById(50)).thenReturn(selectedCard);
        when(action.canDraw(selectedCard, rowsManager)).thenReturn(false);

        phase.onEnter();

        assertThrows(IllegalStateException.class, () -> phase.drawCard(50));
        verify(rowsManager, never()).removeCard(50);
        verify(action, never()).performDraw(selectedCard, rowsManager);
    }

    @Test
    @DisplayName("drawCard should throw when current player cannot acquire selected card")
    void drawCardCannotAcquireThrows() {
        Card selectedCard = mock(Card.class);
        TribeCard legalCard = mock(TribeCard.class);

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(legalCard));
        when(action.canDraw(legalCard, rowsManager)).thenReturn(true);
        when(legalCard.canBeAcquiredBy(p1, model)).thenReturn(true);

        when(rowsManager.findCardById(60)).thenReturn(selectedCard);
        when(action.canDraw(selectedCard, rowsManager)).thenReturn(true);
        when(selectedCard.canBeAcquiredBy(p1, model)).thenReturn(false);

        phase.onEnter();

        assertThrows(IllegalStateException.class, () -> phase.drawCard(60));
        verify(rowsManager, never()).removeCard(60);
        verify(selectedCard, never()).acquiredBy(p1, model);
    }

    @Test
    @DisplayName("onEnter should auto-advance when action is not finished but no legal move exists")
    void onEnterNoLegalMoveAutoAdvances() {
        TribeCard blockedCard = mock(TribeCard.class);

        when(board.getNextPlayerOnOfferTrack()).thenReturn(p1).thenReturn(null);
        when(offerTrack.getOccupiedTileByPlayer(p1)).thenReturn(occupiedTile);
        when(occupiedTile.getAction()).thenReturn(action);

        when(action.isFinished()).thenReturn(false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(blockedCard));
        when(action.canDraw(blockedCard, rowsManager)).thenReturn(true);
        when(blockedCard.canBeAcquiredBy(p1, model)).thenReturn(false);

        phase.onEnter();

        verify(turnOrderTile, times(1)).returnTotemAndResolveEffects(p1);
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

        verify(turnOrderTile, times(1)).returnTotemAndResolveEffects(p1);
        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof PreEndOfRoundPhase));
    }
}


