package model;

import database.ScoreRecord;
import integration.FakeVirtualView;
import model.enums.GamePhase;
import model.player.Player;
import network.server.core.VirtualView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shared.dto.event.EndGameScoringDto;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("GameModel Tests")
class GameModelTest {

    private GameModel gameModel;

    @BeforeEach
    void setUp() {
        FakeVirtualView vv1 = new FakeVirtualView();
        FakeVirtualView vv2 = new FakeVirtualView();

        gameModel = new GameModel(List.of(vv1, vv2));
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
    @DisplayName("notifyEndGame broadcasts game-over to all views; no leaderboard when not set")
    void notifyEndGameBroadcastsGameOver() {
        VirtualView view1 = mock(VirtualView.class);
        VirtualView view2 = mock(VirtualView.class);
        when(view1.getPlayerName()).thenReturn("Player1");
        when(view2.getPlayerName()).thenReturn("Player2");

        GameModel model = new GameModel(List.of(view1, view2));
        model.startGame(List.of("Player1", "Player2"));

        EndGameScoringDto scoring = new EndGameScoringDto(List.of());
        model.setWinners(List.of("Player1"));
        model.setEndGameScoring(scoring);

        model.notifyEndGame();

        verify(view1, times(1)).sendGameOver(List.of("Player1"), scoring);
        verify(view2, times(1)).sendGameOver(List.of("Player1"), scoring);
        verify(view1, never()).sendLeaderboard(any(), anyInt(), anyInt());
        verify(view2, never()).sendLeaderboard(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("notifyEndGame sends the personalised leaderboard plus game-over when a leaderboard is cached")
    void notifyEndGameSendsLeaderboard() {
        VirtualView view1 = mock(VirtualView.class);
        VirtualView view2 = mock(VirtualView.class);
        when(view1.getPlayerName()).thenReturn("Player1");
        when(view2.getPlayerName()).thenReturn("Player2");

        GameModel model = new GameModel(List.of(view1, view2));
        model.startGame(List.of("Player1", "Player2"));

        List<ScoreRecord> top = List.of(new ScoreRecord("Player1", 30, 2, null));
        Map<String, Integer> rankByName = Map.of("Player1", 1, "Player2", 5);
        model.setLeaderboard(new GameModel.LeaderboardData(top, rankByName));
        model.setWinners(List.of("Player1"));
        model.setEndGameScoring(null);

        model.notifyEndGame();

        // points come from each player's prestige (0 for freshly created players)
        verify(view1, times(1)).sendLeaderboard(top, 1, 0);
        verify(view2, times(1)).sendLeaderboard(top, 5, 0);
        verify(view1, times(1)).sendGameOver(List.of("Player1"), null);
        verify(view2, times(1)).sendGameOver(List.of("Player1"), null);
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
