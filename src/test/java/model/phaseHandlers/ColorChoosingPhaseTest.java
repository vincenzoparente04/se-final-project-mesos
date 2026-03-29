package model.phaseHandlers;

import model.GameModel;
import model.enums.TotemColor;
import model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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

    // ──────────────────────────────────────────────────────────────────────────────
    // Setup and Teardown
    // ──────────────────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // Initialize mocks
        try (var ignored = MockitoAnnotations.openMocks(this)) {
            // Setup player list
            List<Player> playerList = new ArrayList<>();
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
    @DisplayName("chooseColor() should assign correct color to player")
    void testChooseColorAssignsColorToPlayer() {
        // Arrange
        colorChoosingPhase.onEnter();

        // Act
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);

        // Assert
        verify(player1, times(1)).setColor(TotemColor.RED);
    }


    @Test
    @DisplayName("chooseColor() should notify observers about color choice")
    void testChooseColorNotifiesObserversAboutChoice() {
        // Arrange
        colorChoosingPhase.onEnter();

        // Act
        colorChoosingPhase.chooseColor(player1, TotemColor.BLUE);

        // Assert - verify that player1 got the color
        verify(player1, times(1)).setColor(TotemColor.BLUE);
        // Verify that notifyChange was called with the color choice
        verify(gameModel, times(1)).notifyChange("color_chosen:Player1:BLUE");
    }

    @Test
    @DisplayName("chooseColor() should advance and notify next player about their turn")
    void testChooseColorNotifiesNextPlayerTurn() {
        // Arrange
        colorChoosingPhase.onEnter();
        assertEquals(player1, colorChoosingPhase.getCurrentPlayer());

        // Act
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);

        // Assert - verify that the current player has advanced
        assertEquals(player2, colorChoosingPhase.getCurrentPlayer(),
                "Current player should advance to player2 after player1 chooses");
        // Verify that notifyChange was called to notify the next player
        verify(gameModel, times(1)).notifyChange("color_choosing_next:Player2");
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // chooseColor() Tests - Error Cases
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("chooseColor() should not process color choice when wrong player tries to choose")
    void testChooseColorIgnoresWrongPlayer() {
        // Arrange
        colorChoosingPhase.onEnter();

        // Act
        colorChoosingPhase.chooseColor(player2, TotemColor.RED);

        // Assert - player2 should not have setColor called
        verify(player2, never()).setColor(any());
        // current player should still be player1
        assertEquals(player1, colorChoosingPhase.getCurrentPlayer());
    }

    @Test
    @DisplayName("chooseColor() should not process color choice when unavailable color is selected")
    void testChooseColorIgnoresUnavailableColor() {
        // Arrange
        colorChoosingPhase.onEnter();
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);

        // Act - player2 tries to choose already taken color
        colorChoosingPhase.chooseColor(player2, TotemColor.RED);

        // Assert - player2 should not have setColor called
        verify(player2, never()).setColor(any());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Multiple Choice Sequence Tests
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("All players should be able to choose colors in sequence")
    void testSequentialColorChoiceForAllPlayers() {
        // Arrange
        colorChoosingPhase.onEnter();

        // Act - All players choose colors
        colorChoosingPhase.chooseColor(player1, TotemColor.RED);
        colorChoosingPhase.chooseColor(player2, TotemColor.BLUE);
        colorChoosingPhase.chooseColor(player3, TotemColor.GREEN);

        // Assert - Each player's setColor should be called exactly once
        verify(player1, times(1)).setColor(TotemColor.RED);
        verify(player2, times(1)).setColor(TotemColor.BLUE);
        verify(player3, times(1)).setColor(TotemColor.GREEN);
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

        // Assert - verify that setPhase was called (indicating completion)
        verify(gameModel, times(1)).setPhase(any(SetupPhase.class));
        // Verify that notifyChange was called to signal completion
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
        localPhase.chooseColor(localPlayer5, TotemColor.RED);
        
        // Verify that setColor was not called for player5 with RED
        verify(localPlayer5, never()).setColor(TotemColor.RED);
    }

}
