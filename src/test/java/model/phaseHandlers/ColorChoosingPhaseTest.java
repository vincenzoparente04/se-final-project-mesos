package model.phaseHandlers;

import model.GameModel;
import model.enums.TotemColor;
import model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ColorChoosingPhase Tests")
public class ColorChoosingPhaseTest {

    // ──────────────────────────────────────────────────────────────────────────────
    // Test Fixtures
    // ──────────────────────────────────────────────────────────────────────────────

    @Mock
    private GameModel gameModel;

    @Mock
    private Player player1;

    @Mock
    private Player player2;

    @Mock
    private Player player3;

    private ColorChoosingPhase colorChoosingPhase;
    private List<Player> playerList;

    // ──────────────────────────────────────────────────────────────────────────────
    // Setup and Teardown
    // ──────────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Setup player list
        playerList = new ArrayList<>();
        playerList.add(player1);
        playerList.add(player2);
        playerList.add(player3);

        // Configure mock behavior
        when(gameModel.getPlayers()).thenReturn(playerList);
        when(gameModel.getPlayerCount()).thenReturn(playerList.size());

        // Setup player names for better debugging
        when(player1.getName()).thenReturn("Player1");
        when(player2.getName()).thenReturn("Player2");
        when(player3.getName()).thenReturn("Player3");

        // Create instance of ColorChoosingPhase
        colorChoosingPhase = new ColorChoosingPhase(gameModel);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // onEnter() Tests
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("onEnter() should initialize available colors and set first player")
    void testOnEnterInitializesGameState() {
        // Arrange - already done in setUp()

        // Act
        colorChoosingPhase.onEnter();

        // Assert - verify that the first player is set correctly
        assertEquals(player1, colorChoosingPhase.getCurrentPlayer(),
                "First player should be set to player1 after onEnter()");

        // Verify that notifyChange was called to notify color choosing started
        verify(gameModel, times(1)).notifyChange(contains("color_choosing_started"));
    }

    @Test
    @DisplayName("onEnter() should notify observers about color choosing start")
    void testOnEnterNotifiesObservers() {
        // Act
        colorChoosingPhase.onEnter();

        // Assert
        verify(gameModel, times(1)).notifyChange("color_choosing_started:Player1");
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // chooseColor() Tests - Valid Cases
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("chooseColor() should assign totem with correct color to player")
    void testChooseColorAssignsTotemToPlayer() {
        // Arrange
        colorChoosingPhase.onEnter();
        ArgumentCaptor<Totem> totemCaptor = ArgumentCaptor.forClass(Totem.class);

        // Act
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);

        // Assert
        verify(player1, times(1)).setTotem(totemCaptor.capture());

        // Verify the correct color was assigned
        Totem capturedTotem = totemCaptor.getValue();
        assertEquals(TotemColor.RED, capturedTotem.getColor(),
                "Totem should have RED color");
        assertEquals(player1, capturedTotem.getPlayer(),
                "Totem should belong to player1");
    }


    @Test
    @DisplayName("chooseColor() should notify observers about color choice")
    void testChooseColorNotifiesObserversAboutChoice() {
        // Arrange
        colorChoosingPhase.onEnter();

        // Act
        colorChoosingPhase.chooseColor(player1, TotemColor.BLUE);

        // Assert
        verify(gameModel, times(1)).notifyChange("color_chosen:Player1:BLUE");
    }

    @Test
    @DisplayName("chooseColor() should advance to next player after valid choice")
    void testChooseColorAdvancesToNextPlayer() {
        // Arrange
        colorChoosingPhase.onEnter();
        assertEquals(player1, colorChoosingPhase.getCurrentPlayer());

        // Act
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);

        // Assert
        assertEquals(player2, colorChoosingPhase.getCurrentPlayer(),
                "Current player should advance to player2 after player1 chooses color");
    }

    @Test
    @DisplayName("chooseColor() should notify next player about their turn")
    void testChooseColorNotifiesNextPlayerTurn() {
        // Arrange
        colorChoosingPhase.onEnter();

        // Act
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);

        // Assert
        verify(gameModel, times(1)).notifyChange("color_choosing_next:Player2");
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // chooseColor() Tests - Error Cases
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("chooseColor() should throw exception when wrong player tries to choose")
    void testChooseColorThrowsExceptionForWrongPlayer() {
        // Arrange
        colorChoosingPhase.onEnter();

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> colorChoosingPhase.chooseColor(player2, TotemColor.RED),
                "Should throw exception when non-current player tries to choose color"
        );

        // Verify exception message contains relevant information
        assertTrue(exception.getMessage().contains("It's not Player2's turn"),
                "Exception message should indicate whose turn it is");
        assertTrue(exception.getMessage().contains("Player1"),
                "Exception message should mention the current player");
    }

    @Test
    @DisplayName("chooseColor() should throw exception for unavailable color")
    void testChooseColorThrowsExceptionForUnavailableColor() {
        // Arrange
        colorChoosingPhase.onEnter();
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> colorChoosingPhase.chooseColor(player2, TotemColor.RED),
                "Should throw exception when trying to choose already-chosen color"
        );

        // Verify exception message contains relevant information
        assertTrue(exception.getMessage().contains("not available"),
                "Exception message should indicate color is unavailable");
        assertTrue(exception.getMessage().contains("RED"),
                "Exception message should mention the unavailable color");
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Multiple Choice Sequence Tests
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("All players should be able to choose colors in sequence")
    void testSequentialColorChoiceForAllPlayers() {
        // Arrange
        colorChoosingPhase.onEnter();
        ArgumentCaptor<Totem> player1TotemCaptor = ArgumentCaptor.forClass(Totem.class);
        ArgumentCaptor<Totem> player2TotemCaptor = ArgumentCaptor.forClass(Totem.class);
        ArgumentCaptor<Totem> player3TotemCaptor = ArgumentCaptor.forClass(Totem.class);

        // Act - All players choose colors
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);
        colorChoosingPhase.chooseColor(player2, TotemColor.BLUE);
        colorChoosingPhase.chooseColor(player3, TotemColor.GREEN);

        // Assert - Each player must receive exactly one totem
        verify(player1, times(1)).setTotem(player1TotemCaptor.capture());
        verify(player2, times(1)).setTotem(player2TotemCaptor.capture());
        verify(player3, times(1)).setTotem(player3TotemCaptor.capture());

        // Assert - Verify both assigned color and owner for each captured totem
        Totem player1Totem = player1TotemCaptor.getValue();
        Totem player2Totem = player2TotemCaptor.getValue();
        Totem player3Totem = player3TotemCaptor.getValue();

        assertEquals(TotemColor.RED, player1Totem.getColor(),
            "Player1 should receive RED totem");
        assertEquals(player1, player1Totem.getPlayer(),
            "Player1 totem should belong to player1");

        assertEquals(TotemColor.BLUE, player2Totem.getColor(),
            "Player2 should receive BLUE totem");
        assertEquals(player2, player2Totem.getPlayer(),
            "Player2 totem should belong to player2");

        assertEquals(TotemColor.GREEN, player3Totem.getColor(),
            "Player3 should receive GREEN totem");
        assertEquals(player3, player3Totem.getPlayer(),
            "Player3 totem should belong to player3");
    }

    @Test
    @DisplayName("Phase should transition to SetupPhase after all players choose colors")
    void testPhaseTransitionToSetupPhaseAfterAllChoose() {
        // Arrange
        colorChoosingPhase.onEnter();

        // Act - All players choose colors
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);
        colorChoosingPhase.chooseColor(player2, TotemColor.BLUE);
        colorChoosingPhase.chooseColor(player3, TotemColor.GREEN);

        // Assert - verify that setPhase was called with a SetupPhase instance
        verify(gameModel, times(1)).setPhase(any(SetupPhase.class));
    }

    @Test
    @DisplayName("Phase should not transition to SetupPhase before last player chooses")
    void testPhaseDoesNotTransitionBeforeLastChoice() {
        // Arrange
        colorChoosingPhase.onEnter();

        // Act - First player chooses
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);

        // Assert - No transition yet
        verify(gameModel, never()).setPhase(any(SetupPhase.class));

        // Act - Second player chooses
        colorChoosingPhase.chooseColor(player2, TotemColor.BLUE);

        // Assert - Still no transition
        verify(gameModel, never()).setPhase(any(SetupPhase.class));

        // Act - Last player chooses
        colorChoosingPhase.chooseColor(player3, TotemColor.GREEN);

        // Assert - Transition happens only now
        verify(gameModel, times(1)).setPhase(any(SetupPhase.class));
    }

    @Test
    @DisplayName("Should notify completion when all players finish choosing colors")
    void testNotifyCompletionWhenAllPlayersChoose() {
        // Arrange
        colorChoosingPhase.onEnter();

        // Act - All players choose colors
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);
        colorChoosingPhase.chooseColor(player2, TotemColor.BLUE);
        colorChoosingPhase.chooseColor(player3, TotemColor.GREEN);

        // Assert
        verify(gameModel, times(1)).notifyChange("color_choosing_completed");
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Max player test
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Each color should only be available once across all players")
    void testColorUniquenessAcrossAllPlayers() {
        // Arrange - Use a dedicated 5-player setup for this uniqueness scenario
        GameModel localModel = mock(GameModel.class);
        Player localPlayer1 = mock(Player.class);
        Player localPlayer2 = mock(Player.class);
        Player localPlayer3 = mock(Player.class);
        Player localPlayer4 = mock(Player.class);
        Player localPlayer5 = mock(Player.class);

        when(localPlayer1.getName()).thenReturn("Player1");
        when(localPlayer2.getName()).thenReturn("Player2");
        when(localPlayer3.getName()).thenReturn("Player3");
        when(localPlayer4.getName()).thenReturn("Player4");
        when(localPlayer5.getName()).thenReturn("Player5");

        List<Player> localPlayers = List.of(localPlayer1, localPlayer2, localPlayer3, localPlayer4, localPlayer5);
        when(localModel.getPlayers()).thenReturn(localPlayers);
        when(localModel.getPlayerCount()).thenReturn(localPlayers.size());

        ColorChoosingPhase localPhase = new ColorChoosingPhase(localModel);
        localPhase.onEnter();

        // Act - First four players choose different colors
        localPhase.chooseColor(localPlayer1, TotemColor.RED);
        localPhase.chooseColor(localPlayer2, TotemColor.BLUE);
        localPhase.chooseColor(localPlayer3, TotemColor.GREEN);
        localPhase.chooseColor(localPlayer4, TotemColor.YELLOW);

        // Assert - Fifth player cannot choose a color that is already taken
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> localPhase.chooseColor(localPlayer5, TotemColor.RED),
                "A previously chosen color should not be available anymore"
        );
        assertTrue(exception.getMessage().contains("not available"),
                "Exception message should indicate color is unavailable");
        assertTrue(exception.getMessage().contains("RED"),
                "Exception message should mention the duplicated color");
        verify(localPlayer5, never()).setTotem(any(Totem.class));
    }

}
