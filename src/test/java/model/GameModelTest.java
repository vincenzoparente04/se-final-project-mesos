package model;

import model.enums.GamePhase;
import model.player.Player;
import network.server.core.VirtualView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("GameModel Tests")
class GameModelTest {

    private GameModel gameModel;

    @BeforeEach
    void setUp() {
        gameModel = new GameModel();
    }

    @Test
    @DisplayName("GameModel is created successfully")
    void gameModelIsCreatedSuccessfully() {
        assertNotNull(gameModel);
    }

    @Test
    @DisplayName("notifyChange sends state to all registered views")
    void notifyChangeSendsStateToAllRegisteredViews() {
        VirtualView view1 = mock(VirtualView.class);
        VirtualView view2 = mock(VirtualView.class);

        GameModel model = new GameModel(List.of(view1, view2));
        model.startGame(List.of("Player1", "Player2"));

        model.notifyChange();

        verify(view1, atLeastOnce()).sendState(any());
        verify(view2, atLeastOnce()).sendState(any());
    }

    @Test
    @DisplayName("swapView replaces the old view for a player: old is silent, new receives notifications")
    void swapViewReplacesOldViewWithNew() {
        VirtualView oldView = mock(VirtualView.class);
        VirtualView newView = mock(VirtualView.class);
        when(oldView.getPlayerName()).thenReturn("Player1");
        when(newView.getPlayerName()).thenReturn("Player1");

        GameModel model = new GameModel(List.of(oldView));
        model.startGame(List.of("Player1", "Player2"));
        clearInvocations(oldView);

        model.swapView("Player1", newView);
        model.notifyChange();

        verify(oldView, never()).sendState(any());
        verify(newView, times(1)).sendState(any());
    }

    @Test
    @DisplayName("removeView removes the view for the given player")
    void removeViewRemovesPlayerView() {
        VirtualView view = mock(VirtualView.class);
        when(view.getPlayerName()).thenReturn("Player1");

        GameModel model = new GameModel(List.of(view));
        model.startGame(List.of("Player1", "Player2"));
        clearInvocations(view);

        model.removeView("Player1");
        model.notifyChange();

        verify(view, never()).sendState(any());
        assertTrue(model.getViews().isEmpty());
    }

    @Test
    @DisplayName("getPlayerByName throws IllegalArgumentException for an unknown player name")
    void getPlayerByNameThrowsForUnknownPlayer() {
        gameModel.startGame(List.of("Player1", "Player2"));

        assertThrows(IllegalArgumentException.class,
                () -> gameModel.getPlayerByName("PierlucaAttilioPrimicieri"));
    }

    @Test
    @DisplayName("isGameOver returns false when currentRound equals max rounds (10)")
    void isGameOverReturnsFalseAtRound10() {
        for (int i = 0; i < 9; i++) gameModel.incrementRound();

        assertFalse(gameModel.isGameOver());
        assertEquals(10, gameModel.getCurrentRound());
    }

    @Test
    @DisplayName("isGameOver returns true when currentRound exceeds max rounds (11)")
    void isGameOverReturnsTrueAtRound11() {
        for (int i = 0; i < 10; i++) gameModel.incrementRound();

        assertTrue(gameModel.isGameOver());
        assertEquals(11, gameModel.getCurrentRound());
    }


    @Test
    @DisplayName("gameModel")
    void gameModelEnzo() {
        VirtualView view1 = mock(VirtualView.class);
        VirtualView view2 = mock(VirtualView.class);
        GameModel gameModel2 = new GameModel(List.of(view1, view2));

        gameModel2.startGame(List.of("Player1", "Player2"));

        gameModel2.notifyChange();

        verify(view1, atLeastOnce()).sendState(any());
        verify(view2, atLeastOnce()).sendState(any());
    }

    @Test
    @DisplayName("gameModel getters null")
    void gameModelGetters() {
        GamePhase phase = gameModel.getCurrentPhase();
        assertNull(phase);

        Player player = gameModel.getCurrentPlayer();
        assertNull(player);
    }
}
