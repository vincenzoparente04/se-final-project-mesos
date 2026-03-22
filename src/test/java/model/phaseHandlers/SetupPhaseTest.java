package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.board.TurnOrderTile;
import model.enums.GamePhase;
import model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Note: these tests are pretty much just verifying that the flow of the onEnter method is correct, since all the actual logic is in Board and Player methods which have their own tests.

public class SetupPhaseTest {

	private GameModel model;
	private Board board;
	private TurnOrderTile turnOrderTile;
	private SetupPhase phase;

	@BeforeEach
	void setUp() {
		model = mock(GameModel.class);
		board = mock(Board.class);
		turnOrderTile = mock(TurnOrderTile.class);
		phase = new SetupPhase(model);

		when(model.getBoard()).thenReturn(board);
		when(board.getTurnOrderTile()).thenReturn(turnOrderTile);
	}

	@Test
	@DisplayName("onEnter runs board setup, randomizes turn order, distributes food, and transitions to PlacementPhase")
	void onEnterRunsSetupFlowAndTransitionsToPlacement() {
		Player p1 = mock(Player.class);
		Player p2 = mock(Player.class);
		Player p3 = mock(Player.class);
		Player p4 = mock(Player.class);
		Player p5 = mock(Player.class);

		List<Player> allPlayers = List.of(p1, p2, p3, p4, p5);
		when(model.getPlayerCount()).thenReturn(5);
		when(model.getPlayers()).thenReturn(allPlayers);
		when(turnOrderTile.getTurnOrder()).thenReturn(allPlayers);

		phase.onEnter();

		var order = inOrder(board, model);
		order.verify(board).setup(5);
		order.verify(board).randomizeTurnOrder(allPlayers);
		order.verify(model).setPhase(argThat(handler -> handler instanceof PlacementPhase));

		verify(p1).addFood(2);
		verify(p2).addFood(3);
		verify(p3).addFood(3);
		verify(p4).addFood(4);
		verify(p5).addFood(4);
	}

	@Test
	@DisplayName("onEnter with 3 players assigns only 2,3,3 food bonuses")
	void onEnterWithThreePlayersDistributesCorrectFood() {
		Player p1 = mock(Player.class);
		Player p2 = mock(Player.class);
		Player p3 = mock(Player.class);

		List<Player> players = List.of(p1, p2, p3);
		when(model.getPlayerCount()).thenReturn(3);
		when(model.getPlayers()).thenReturn(players);
		when(turnOrderTile.getTurnOrder()).thenReturn(players);

		phase.onEnter();

		verify(p1).addFood(2);
		verify(p2).addFood(3);
		verify(p3).addFood(3);
		verify(p1, never()).addFood(4);
		verify(p2, never()).addFood(4);
		verify(p3, never()).addFood(4);
	}

	@Test
	@DisplayName("onEnter distributes food using turn order, not original player list order")
	void onEnterDistributesFoodByTurnOrder() {
		Player p1 = mock(Player.class);
		Player p2 = mock(Player.class);
		Player p3 = mock(Player.class);
		Player p4 = mock(Player.class);

		List<Player> originalPlayers = List.of(p1, p2, p3, p4);
		List<Player> turnOrderPlayers = List.of(p4, p2, p1, p3);

		when(model.getPlayerCount()).thenReturn(4);
		when(model.getPlayers()).thenReturn(originalPlayers);
		when(turnOrderTile.getTurnOrder()).thenReturn(turnOrderPlayers);

		phase.onEnter();

		var order = inOrder(board, model, p4, p2, p1, p3);
		order.verify(board).setup(4);
		order.verify(board).randomizeTurnOrder(originalPlayers);
		order.verify(p4).addFood(2);
		order.verify(p2).addFood(3);
		order.verify(p1).addFood(3);
		order.verify(p3).addFood(4);
		order.verify(model).setPhase(argThat(handler -> handler instanceof PlacementPhase));

		verify(p4).addFood(2);
		verify(p2).addFood(3);
		verify(p1).addFood(3);
		verify(p3).addFood(4);

		verify(p1, never()).addFood(2);
		verify(p4, never()).addFood(4);
	}

}
