package model.phaseHandlers;

import model.GameModel;
import model.cards.characterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.enums.GamePhase;
import model.player.Player;
import model.player.Tribe;
import model.rowsManager.CardVisitor;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shared.command.gameCommand.DrawCardCommand;
import shared.command.gameCommand.EndTurnCommand;

import java.util.List;
import java.util.Optional;

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
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(card));
        when(rowsManager.topRowContainsCard(card.getId())).thenReturn(true);

        phase.onEnter();

        assertEquals(p2, phase.getCurrentPlayer());
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("visit(DrawCardCommand) should do nothing when there is no active player")
    void drawCardWithoutActivePlayerDoesNothing() throws Exception {
        phase.visit(new DrawCardCommand("Player1", 10));

        verify(rowsManager, never()).findCardById(10);
        verify(rowsManager, never()).removeCard(10);
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("visit(DrawCardCommand) should throw when card is not found")
    void drawCardMissingCardDoesNothing() throws Exception {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        when(rowsManager.findCardById(10)).thenReturn(Optional.empty());
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(card));
        when(rowsManager.topRowContainsCard(card.getId())).thenReturn(true);

        phase.onEnter();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> phase.visit(new DrawCardCommand("Player1", 10)));

        verify(rowsManager, times(1)).findCardById(10);
        verify(rowsManager, never()).removeCard(10);
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("visit(DrawCardCommand) should throw when card is not in top row")
    void drawCardCardNotInTopRowDoesNothing() throws Exception {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        when(rowsManager.findCardById(10)).thenReturn(Optional.of(card));
        // true for MoveChecker in onEnter, false for topRow check in visit()
        when(rowsManager.topRowContainsCard(10)).thenReturn(true, false);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(card));

        phase.onEnter();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> phase.visit(new DrawCardCommand("Player1", 10)));

        verify(rowsManager, times(1)).findCardById(10);
        verify(rowsManager, times(2)).topRowContainsCard(10); // once in MoveChecker (onEnter), once in visit
        verify(rowsManager, never()).removeCard(10);
        verify(card, never()).registerToTribe(p1);
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("visit(DrawCardCommand) should throw and stay in phase when selected card is an event")
    void drawCardEventCardThrows() throws Exception {
        EventCard eventCard = mock(EventCard.class);
        when(eventCard.getId()).thenReturn(10);
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            visitor.visit(eventCard);
            return null;
        }).when(eventCard).accept(org.mockito.ArgumentMatchers.any(CardVisitor.class));

        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        when(rowsManager.findCardById(10)).thenReturn(Optional.of(eventCard));
        when(rowsManager.topRowContainsCard(10)).thenReturn(true);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(card));

        phase.onEnter();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> phase.visit(new DrawCardCommand("Player1", 10)));

        verify(rowsManager, never()).removeCard(10);
        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("visit(DrawCardCommand) should remove card, acquire it and transition to EndOfRoundPhase")
    void drawCardValidFlowTransitionsToEndOfRound() throws Exception {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        when(rowsManager.findCardById(10)).thenReturn(Optional.of(card));
        when(rowsManager.topRowContainsCard(10)).thenReturn(true);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(card));

        phase.onEnter();
        phase.visit(new DrawCardCommand("Player1", 10));

        verify(rowsManager, times(1)).removeCard(10);
        verify(card, times(1)).registerToTribe(p1);
        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("visit(EndTurnCommand) should transition when there is an active player")
    void endTurnWithActivePlayerTransitions() throws Exception {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        when(rowsManager.getAllCardsOnBoard()).thenReturn(List.of(card));
        when(rowsManager.topRowContainsCard(card.getId())).thenReturn(true);

        phase.onEnter();
        phase.visit(new EndTurnCommand("Player1"));

        verify(model, times(1)).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("visit(EndTurnCommand) should do nothing when there is no active player")
    void endTurnWithoutActivePlayerDoesNothing() throws Exception {
        phase.visit(new EndTurnCommand("Player1"));

        verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfRoundPhase));
    }

    @Test
    @DisplayName("preEndOfRoundGetter")
    void preEndOfRoundGetter() {
        when(model.getPlayers()).thenReturn(List.of(p1));
        when(p1.hasExtraDraw()).thenReturn(true);
        phase.onEnter();

        assertEquals(GamePhase.PRE_END_OF_ROUND, phase.getPhase());
    }

}
