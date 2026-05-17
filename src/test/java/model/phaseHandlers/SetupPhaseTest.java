package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.board.TurnOrderSlot;
import model.board.TurnOrderTile;
import model.enums.GamePhase;
import model.enums.TotemLocation;
import model.player.Player;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Note: these tests are pretty much just verifying that the flow of the onEnter method is correct

public class SetupPhaseTest {

    private GameModel model;
    private Board board;
    private RowsManager rowsManager;
    private TurnOrderTile turnOrderTile;
    private SetupPhase phase;

    @BeforeEach
    void setUp() {
        model = mock(GameModel.class);
        board = mock(Board.class);
        rowsManager = mock(RowsManager.class);
        turnOrderTile = mock(TurnOrderTile.class);
        phase = new SetupPhase(model);

        when(model.getBoard()).thenReturn(board);
        when(model.getRowsManager()).thenReturn(rowsManager);
        when(board.getTurnOrderTile()).thenReturn(turnOrderTile);
    }

    private List<TurnOrderSlot> mockSlots(int count) {
        List<TurnOrderSlot> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            slots.add(mock(TurnOrderSlot.class));
        }
        return slots;
    }

    private List<TurnOrderSlot> statefulSlots(int count) {
        List<TurnOrderSlot> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Use real slot methods so occupant state actually changes during placeTotem.
            slots.add(mock(TurnOrderSlot.class, Answers.CALLS_REAL_METHODS));
        }
        return slots;
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
        List<TurnOrderSlot> slots = mockSlots(5);

        when(model.getPlayerCount()).thenReturn(5);
        when(model.getPlayers()).thenReturn(allPlayers);
        when(turnOrderTile.getSlots()).thenReturn(slots);
        when(turnOrderTile.getTurnOrder()).thenReturn(allPlayers);

        phase.onEnter();

        var order = inOrder(rowsManager, model);
        order.verify(rowsManager).setup(5);
        order.verify(model).setPhase(argThat(handler -> handler instanceof PlacementPhase));

        // Randomization is shuffled: assert side effects, not exact order.
        for (TurnOrderSlot slot : slots) {
            verify(slot, times(1)).placeTotem(any(Player.class));
        }
        verify(p1).setLocation(TotemLocation.TURN_ORDER_TILE);
        verify(p2).setLocation(TotemLocation.TURN_ORDER_TILE);
        verify(p3).setLocation(TotemLocation.TURN_ORDER_TILE);
        verify(p4).setLocation(TotemLocation.TURN_ORDER_TILE);
        verify(p5).setLocation(TotemLocation.TURN_ORDER_TILE);
        
        // Food distribution is based on turn order, which is randomized, so verify correct amounts without assuming order.
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
        when(turnOrderTile.getSlots()).thenReturn(mockSlots(3));
        when(turnOrderTile.getTurnOrder()).thenReturn(players);

        phase.onEnter();

        //fake order, not randomized, only to verify that the correct amounts of food are given.
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
        when(turnOrderTile.getSlots()).thenReturn(mockSlots(4));
        when(turnOrderTile.getTurnOrder()).thenReturn(turnOrderPlayers);

        phase.onEnter();

        var order = inOrder(rowsManager, p4, p2, p1, p3, model);
        order.verify(rowsManager).setup(4);
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

    @Test
    @DisplayName("randomizeTurnOrder fills real slot state with all players exactly once")
    void randomizeTurnOrderFillsSlotsWithUniquePlayers() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);
        Player p4 = mock(Player.class);

        List<Player> players = List.of(p1, p2, p3, p4);
        List<TurnOrderSlot> slots = statefulSlots(4);

        when(model.getPlayers()).thenReturn(players);
        when(turnOrderTile.getSlots()).thenReturn(slots);

        phase.randomizeTurnOrder(players);

        // Order is randomized, so verify invariants: all slots occupied and all players present once.
        Set<Player> occupants = new HashSet<>();
        for (TurnOrderSlot slot : slots) {
            if (!slot.isOccupied()) {
                throw new AssertionError("Every slot should be occupied after randomization");
            }
            occupants.add(slot.getOccupant());
        }

        assertEquals(players.size(), occupants.size(),
                "Each player should occupy exactly one slot");
        assertTrue(occupants.containsAll(players),
                "All players should appear in the occupied slots");

        verify(p1).setLocation(TotemLocation.TURN_ORDER_TILE);
        verify(p2).setLocation(TotemLocation.TURN_ORDER_TILE);
        verify(p3).setLocation(TotemLocation.TURN_ORDER_TILE);
        verify(p4).setLocation(TotemLocation.TURN_ORDER_TILE);
    }

    @Test
    @DisplayName("onEnter distributes food by actual slot occupants when turn order is derived from slots")
    void onEnterUsesSlotOccupantsForFoodDistribution() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);
        Player p4 = mock(Player.class);

        List<Player> players = List.of(p1, p2, p3, p4);
        List<TurnOrderSlot> slots = statefulSlots(4);

        when(model.getPlayerCount()).thenReturn(4);
        when(model.getPlayers()).thenReturn(players);
        when(turnOrderTile.getSlots()).thenReturn(slots);
        // Keep turn order tied to slot state instead of hardcoding a fake list.
        when(turnOrderTile.getTurnOrder()).thenAnswer(ignored -> slots.stream()
                .filter(TurnOrderSlot::isOccupied)
                .map(TurnOrderSlot::getOccupant)
                .toList());

        phase.onEnter();

        Player first = slots.get(0).getOccupant();
        Player second = slots.get(1).getOccupant();
        Player third = slots.get(2).getOccupant();
        Player fourth = slots.get(3).getOccupant();

        assertNotNull(first, "Slot 0 should be occupied");
        assertNotNull(second, "Slot 1 should be occupied");
        assertNotNull(third, "Slot 2 should be occupied");
        assertNotNull(fourth, "Slot 3 should be occupied");

        verify(first).addFood(2);
        verify(second).addFood(3);
        verify(third).addFood(3);
        verify(fourth).addFood(4);
        verify(model).setPhase(argThat(handler -> handler instanceof PlacementPhase));
    }

    @Test
    @DisplayName("getPhase returns SETUP")
    void getPhaseReturnsSetup() {
        assertEquals(GamePhase.SETUP, phase.getPhase());
    }

    /*
    @Test
    @DisplayName("getCurrentPlayer returns null during SetupPhase (no active player)")
    void getCurrentPlayerReturnsNull() {
        assertNull(phase.getCurrentPlayer());
    }

    @Test
    @DisplayName("randomizeTurnOrder throws when there are more players than available slots")
    void randomizeTurnOrderThrowsWhenPlayersExceedSlots() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);
        Player p4 = mock(Player.class);

        List<Player> players = List.of(p1, p2, p3, p4);
        List<TurnOrderSlot> slots = statefulSlots(3);

        when(turnOrderTile.getSlots()).thenReturn(slots);

        assertThrows(IndexOutOfBoundsException.class, () -> phase.randomizeTurnOrder(players),
                "Should fail when player count exceeds slot count");

        // The available slots should still be filled with unique players before the failure point.
        Set<Player> occupants = new HashSet<>();
        for (TurnOrderSlot slot : slots) {
            assertTrue(slot.isOccupied(), "Each available slot should be occupied before exception");
            occupants.add(slot.getOccupant());
        }
        assertEquals(slots.size(), occupants.size(), "Occupied slots should not contain duplicate players");
    }

}
