package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.enums.GamePhase;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EndOfGamePhaseTest {

	private GameModel model;
	private Board board;
	private EndOfGamePhase phase;

	@BeforeEach
	void setUp() {
		model = mock(GameModel.class);
		board = mock(Board.class);
		phase = new EndOfGamePhase(model);

		when(model.getBoard()).thenReturn(board);
	}

	@Test
	@DisplayName("onEnter resolves final events, computes scores, and notifies single winner")
	void onEnterResolvesEventsScoresAndNotifiesSingleWinner() {
        //setup of the two players and their tribes
		Player p1 = mock(Player.class);
		Player p2 = mock(Player.class);
		Tribe t1 = mock(Tribe.class);
		Tribe t2 = mock(Tribe.class);

		List<Player> players = List.of(p1, p2);
		when(model.getPlayers()).thenReturn(players);

		when(p1.getName()).thenReturn("p1");
		when(p2.getName()).thenReturn("p2");

		when(p1.getTribe()).thenReturn(t1);
		when(p2.getTribe()).thenReturn(t2);

        //setup of the characters and buildings of each player (points)  
		when(t1.calculateBuildersEndGamePoints()).thenReturn(3);
		when(t1.calculateArtistEndGamePoints()).thenReturn(10);
		when(t1.calculateInventorEndGamePoints()).thenReturn(4);
		when(t1.calculateBuildingPrintedPoints()).thenReturn(8);

		when(t2.calculateBuildersEndGamePoints()).thenReturn(2);
		when(t2.calculateArtistEndGamePoints()).thenReturn(0);
		when(t2.calculateInventorEndGamePoints()).thenReturn(2);
		when(t2.calculateBuildingPrintedPoints()).thenReturn(5);

		when(p1.getPrestigePoints()).thenReturn(30);
		when(p2.getPrestigePoints()).thenReturn(24);
        
        //execute
		phase.onEnter();

        //verify (first the order of operations, then the points calculations, then the winner)
		var order = inOrder(board, model);
		order.verify(board, times(1)).resolveAllEvents(players);
		order.verify(model, times(1)).notifyChange("final_events_resolved");
		order.verify(model, times(1)).notifyChange("endgame_scoring_complete");
		order.verify(model, times(1)).notifyChange("game_over:p1");
		verify(model, never()).notifyChange("game_over:p2");

		verify(p1, times(1)).addPrestigePoints(3);
		verify(p1, times(1)).addPrestigePoints(10);
		verify(p1, times(1)).addPrestigePoints(4);
		verify(p1, times(1)).addPrestigePoints(8);
		verify(p1, never()).addPrestigePoints(99); //ahahah

		verify(p2, times(1)).addPrestigePoints(2);
		verify(p2, times(1)).addPrestigePoints(0);
		verify(p2, times(1)).addPrestigePoints(2);
		verify(p2, times(1)).addPrestigePoints(5);
		verify(p2, never()).addPrestigePoints(99);

		assertEquals(1, phase.getWinners().size());
		assertSame(p1, phase.getWinners().getFirst());
	}

	@Test
	@DisplayName("when prestige is tied, player with more food wins")
	void determineWinnerUsesFoodAsTieBreak() {
		Player p1 = mock(Player.class);
		Player p2 = mock(Player.class);
		Tribe t1 = mock(Tribe.class);
		Tribe t2 = mock(Tribe.class);

		List<Player> players = List.of(p1, p2);
		when(model.getPlayers()).thenReturn(players);

		when(p1.getName()).thenReturn("p1");
		when(p2.getName()).thenReturn("p2");

		when(p1.getTribe()).thenReturn(t1);
		when(p2.getTribe()).thenReturn(t2);

		when(p1.getPrestigePoints()).thenReturn(25);
		when(p2.getPrestigePoints()).thenReturn(25);
		when(p1.getFood()).thenReturn(4);
		when(p2.getFood()).thenReturn(7);

		phase.onEnter();

		verify(board, times(1)).resolveAllEvents(players);
		verify(model, times(1)).notifyChange("final_events_resolved");
		verify(model, times(1)).notifyChange("endgame_scoring_complete");
		verify(model, times(1)).notifyChange("game_over:p2");
		verify(model, never()).notifyChange("game_over:p1");
		assertEquals(1, phase.getWinners().size());
		assertSame(p2, phase.getWinners().getFirst());
	}

	@Test
	@DisplayName("if tie remains after tiebreak, victory is shared")
	void determineWinnerSharedVictoryIfStillTiedAfterFood() {
		Player p1 = mock(Player.class);
		Player p2 = mock(Player.class);
		Tribe t1 = mock(Tribe.class);
		Tribe t2 = mock(Tribe.class);

		List<Player> players = List.of(p1, p2);
		when(model.getPlayers()).thenReturn(players);

		when(p1.getName()).thenReturn("p1");
		when(p2.getName()).thenReturn("p2");

		when(p1.getTribe()).thenReturn(t1);
		when(p2.getTribe()).thenReturn(t2);

		when(p1.getPrestigePoints()).thenReturn(40);
		when(p2.getPrestigePoints()).thenReturn(40);
		when(p1.getFood()).thenReturn(6);
		when(p2.getFood()).thenReturn(6);

		phase.onEnter();

		verify(board, times(1)).resolveAllEvents(players);
		verify(model, times(1)).notifyChange("final_events_resolved");
		verify(model, times(1)).notifyChange("endgame_scoring_complete");
		verify(model, times(1)).notifyChange("game_over:p1, p2");
		verify(model, never()).notifyChange("game_over:p1");
		verify(model, never()).notifyChange("game_over:p2");
		assertEquals(2, phase.getWinners().size());
		assertSame(p1, phase.getWinners().get(0));
		assertSame(p2, phase.getWinners().get(1));
	}
}
