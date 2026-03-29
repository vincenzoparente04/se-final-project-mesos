package model.phaseHandlers;

import model.GameModel;
import model.cards.Card;
import model.enums.GamePhase;
import model.player.Player;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PreEndOfRoundPhase Tests")
class PreEndOfRoundPhaseTest {

    private GameModel model;
    private RowsManager rowsManager;

    private Player p1;
    private Player p2;
    private Player p3;

    private Card card;

    private PreEndOfRoundPhase phase;

    @BeforeEach
    void setUp() {
        model = mock(GameModel.class);
        rowsManager = mock(RowsManager.class);

        p1 = mock(Player.class);
        p2 = mock(Player.class);
        p3 = mock(Player.class);

        card = mock(Card.class);

        when(model.getRowsManager()).thenReturn(rowsManager);

        phase = new PreEndOfRoundPhase(model);
    }

    @Test
    @DisplayName("onEnter should transition to EndOfRoundPhase when no player has extra draw")
    void onEnterNoEligiblePlayerTransitionsToEndOfRound() {
        when(model.getPlayers()).thenReturn(List.of(p1, p2, p3));
        when(p1.hasExtraDraw()).thenReturn(false);
        when(p2.hasExtraDraw()).thenReturn(false);
        when(p3.hasExtraDraw()).thenReturn(false);

        phase.onEnter();

        assertNull(phase.getCurrentPlayer());
        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("onEnter should select first player with extra draw")
    void onEnterSelectsFirstEligiblePlayer() {
        when(model.getPlayers()).thenReturn(List.of(p1, p2, p3));
        when(p1.hasExtraDraw()).thenReturn(false);
        when(p2.hasExtraDraw()).thenReturn(true);

        phase.onEnter();

        assertEquals(p2, phase.getCurrentPlayer());
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("drawCard should do nothing when there is no active player")
    void drawCardWithoutActivePlayerDoesNothing() {
        phase.drawCard(10);

        verify(rowsManager, never()).findCardById(10);
        verify(rowsManager, never()).removeCard(10);
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("drawCard should do nothing when card is not found")
    void drawCardMissingCardDoesNothing() {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        when(rowsManager.findCardById(10)).thenReturn(null);

        phase.onEnter();
        phase.drawCard(10);

        verify(rowsManager, times(1)).findCardById(10);
        verify(rowsManager, never()).removeCard(10);
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("drawCard should do nothing when card is not in top row")
    void drawCardCardNotInTopRowDoesNothing() {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        when(rowsManager.findCardById(10)).thenReturn(card);
        when(rowsManager.topRowContainsCard(10)).thenReturn(false);

        phase.onEnter();
        phase.drawCard(10);

        verify(rowsManager, times(1)).findCardById(10);
        verify(rowsManager, times(1)).topRowContainsCard(10);
        verify(rowsManager, never()).removeCard(10);
        verify(card, never()).acquiredBy(p1, model);
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("drawCard should do nothing when active player cannot acquire card")
    void drawCardCannotAcquireDoesNothing() {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        when(rowsManager.findCardById(10)).thenReturn(card);
        when(rowsManager.topRowContainsCard(10)).thenReturn(true);
        when(card.canBeAcquiredBy(p1, model)).thenReturn(false);

        phase.onEnter();
        phase.drawCard(10);

        verify(rowsManager, never()).removeCard(10);
        verify(card, never()).acquiredBy(p1, model);
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("drawCard should remove card, acquire it and transition to EndOfRoundPhase")
    void drawCardValidFlowTransitionsToEndOfRound() {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        when(rowsManager.findCardById(10)).thenReturn(card);
        when(rowsManager.topRowContainsCard(10)).thenReturn(true);
        when(card.canBeAcquiredBy(p1, model)).thenReturn(true);

        phase.onEnter();
        phase.drawCard(10);

        verify(rowsManager, times(1)).removeCard(10);
        verify(card, times(1)).acquiredBy(p1, model);
        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("skipAction should transition when there is an active player")
    void skipActionWithActivePlayerTransitions() {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);

        phase.onEnter();
        phase.skipAction();

        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("skipAction should do nothing when there is no active player")
    void skipActionWithoutActivePlayerDoesNothing() {
        phase.skipAction();

        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

}

