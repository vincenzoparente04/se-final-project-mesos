package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.enums.Era;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EndOfRoundPhaseTest {

	private GameModel model;
	private RowsManager rowsManager;
	private EndOfRoundPhase phase;

	@BeforeEach
	void setUp() {
		model = mock(GameModel.class);
		rowsManager = mock(RowsManager.class);
		Board board = mock(Board.class);
		phase = new EndOfRoundPhase(model);

		when(model.getRowsManager()).thenReturn(rowsManager);
		when(model.getBoard()).thenReturn(board);
	}

	/**
	 * Test that onEnter resolves events, ends the round, and transitions to PlacementPhase
	 * when the game is not over. Verifies correct order of operations.
	 */
	@Test
	@DisplayName("onEnter resolves events, ends round and transitions to placement when game is not over")
	void onEnterNonGameOverMovesToPlacement() {
		// Arrange: setup game state with 2 players, no era change, game not over
		List<Player> players = List.of(mock(Player.class), mock(Player.class));

		when(model.getPlayers()).thenReturn(players);
		when(model.getPlayerCount()).thenReturn(2);
		when(model.getCurrentEra()).thenReturn(Era.ERA_I, Era.ERA_I);
		when(model.isGameOver()).thenReturn(false);

		// Act: call onEnter
		phase.onEnter();

		// Assert: verify correct sequence of operations
		var order = inOrder(rowsManager, model);
		order.verify(rowsManager).resolveEvents(players);
		order.verify(model).notifyChange("events_resolved");
		order.verify(rowsManager).endRound(2);
		order.verify(model).incrementRound();

		// Verify no era change occurred and PlacementPhase is set
		verify(rowsManager, never()).changeEra();
		verify(model).setPhase(argThat(handler -> handler instanceof PlacementPhase));
		verify(model, never()).setPhase(argThat(handler -> handler instanceof EndOfGamePhase));
	}

	/**
	 * Test that onEnter notifies of era change and updates board
	 * when the current era changes during the round end.
	 */
	@Test
	@DisplayName("onEnter notifies and updates board when era changes")
	void onEnterWhenEraChangesNotifiesAndUpdatesBoard() {
		// Arrange: setup game state with era transition from ERA_I to ERA_II
		List<Player> players = List.of(mock(Player.class), mock(Player.class), mock(Player.class));

		when(model.getPlayers()).thenReturn(players);
		when(model.getPlayerCount()).thenReturn(3);
		when(model.getCurrentEra()).thenReturn(Era.ERA_I, Era.ERA_II);
		when(model.isGameOver()).thenReturn(false);

		// Act: call onEnter
		phase.onEnter();

		// Assert: verify era change notification and board update
		verify(model).notifyChange("era_changed:" + Era.ERA_II);
		verify(rowsManager).changeEra();
		verify(model).setPhase(argThat(handler -> handler instanceof PlacementPhase));
	}

	/**
	 * Test that onEnter transitions to EndOfGamePhase when game is over,
	 * without incrementing the round.
	 */
	@Test
	@DisplayName("onEnter transitions to EndOfGamePhase without incrementing round when game is over")
	void onEnterGameOverMovesToEndOfGame() {
		// Arrange: setup game state with game over condition
		List<Player> players = List.of(mock(Player.class));

		when(model.getPlayers()).thenReturn(players);
		when(model.getPlayerCount()).thenReturn(1);
		when(model.getCurrentEra()).thenReturn(Era.ERA_I, Era.ERA_I);
		when(model.isGameOver()).thenReturn(true);

		// Act: call onEnter
		phase.onEnter();

		// Assert: verify EndOfGamePhase is set and round is NOT incremented
		verify(model).setPhase(argThat(handler -> handler instanceof EndOfGamePhase));
		verify(model, never()).incrementRound();
		verify(model, never()).setPhase(argThat(handler -> handler instanceof PlacementPhase));
	}

	/**
	 * Test that getPhase returns the fixed phase constant for this handler.
	 */
	@Test
	@DisplayName("getPhase returns END_OF_ROUND")
	void getPhaseReturnsEndOfRound() {
		assertEquals(GamePhase.END_OF_ROUND, phase.getPhase());
	}

	/**
	 * Test that getCurrentPlayer is always null during EndOfRoundPhase.
	 */
	@Test
	@DisplayName("getCurrentPlayer returns null")
	void getCurrentPlayerReturnsNull() {
		assertNull(phase.getCurrentPlayer());
	}

}
