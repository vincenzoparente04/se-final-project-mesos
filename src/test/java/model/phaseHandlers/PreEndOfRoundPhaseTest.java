package model.phaseHandlers;

import model.GameModel;
import model.cards.charachterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.player.Player;
import model.player.Tribe;
import model.rowsManager.CardVisitor;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
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

    private Tribe t1;
    private Tribe t2;
    private Tribe t3;

    private CharacterCard card;

    private PreEndOfRoundPhase phase;

    @BeforeEach
    void setUp() {
        model = mock(GameModel.class);
        rowsManager = mock(RowsManager.class);

        p1 = mock(Player.class);
        p2 = mock(Player.class);
        p3 = mock(Player.class);

        t1 = mock(Tribe.class);
        t2 = mock(Tribe.class);
        t3 = mock(Tribe.class);

        when(p1.getTribe()).thenReturn(t1);
        when(p2.getTribe()).thenReturn(t2);
        when(p3.getTribe()).thenReturn(t3);

        card = mock(CharacterCard.class);
        when(card.getId()).thenReturn(10);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(card);
            return null;
        }).when(card).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

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
        verify(card, never()).registerToTribe(p1);
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("drawCard should throw and stay in phase when selected card is an event")
    void drawCardEventCardThrows() {
        EventCard eventCard = mock(EventCard.class);
        when(eventCard.getId()).thenReturn(10);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(eventCard);
            return null;
        }).when(eventCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        when(rowsManager.findCardById(10)).thenReturn(eventCard);
        when(rowsManager.topRowContainsCard(10)).thenReturn(true);

        phase.onEnter();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> phase.drawCard(10));

        verify(rowsManager, never()).removeCard(10);
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("drawCard should remove card, acquire it and transition to EndOfRoundPhase")
    void drawCardValidFlowTransitionsToEndOfRound() {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        when(rowsManager.findCardById(10)).thenReturn(card);
        when(rowsManager.topRowContainsCard(10)).thenReturn(true);

        phase.onEnter();
        phase.drawCard(10);

        verify(rowsManager, times(1)).removeCard(10);
        verify(card, times(1)).registerToTribe(p1);
        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("endTurn should transition when there is an active player")
    void endTurnWithActivePlayerTransitions() {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);

        phase.onEnter();
        phase.endTurn();

        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("endTurn should do nothing when there is no active player")
    void endTurnWithoutActivePlayerDoesNothing() {
        phase.endTurn();

        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

}

