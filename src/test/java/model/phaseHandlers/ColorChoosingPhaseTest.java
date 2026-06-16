package model.phaseHandlers;

import integration.FakeVirtualView;
import model.GameModel;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import shared.command.gameCommand.ChooseColorCommand;

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
    void setUp() throws Exception {
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

            // Players are connected
            when(player1.isConnected()).thenReturn(true);
            when(player2.isConnected()).thenReturn(true);
            when(player3.isConnected()).thenReturn(true);

            // Wire getPlayerByName so visit() can resolve the player object
            when(gameModel.getPlayerByName("Player1")).thenReturn(player1);
            when(gameModel.getPlayerByName("Player2")).thenReturn(player2);
            when(gameModel.getPlayerByName("Player3")).thenReturn(player3);

            // Create instance of ColorChoosingPhase
            colorChoosingPhase = new ColorChoosingPhase(gameModel);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // onEnter() Tests
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("onEnter() should initialize available colors (no turn tracking)")
    void testOnEnterInitializesGameState() {
        colorChoosingPhase.onEnter();

        assertNull(colorChoosingPhase.getCurrentPlayer());
    }

    @Test
    @DisplayName("onEnter() should notify observers about color choosing start")
    void testOnEnterNotifiesObservers() {
        colorChoosingPhase.onEnter();

        verify(gameModel, times(1)).notifyChange();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // visit(ChooseColorCommand) Tests - Valid Cases
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("visit(ChooseColorCommand) should assign correct color to player")
    void testChooseColorAssignsColorToPlayer() throws Exception {
        colorChoosingPhase.onEnter();

        colorChoosingPhase.visit(new ChooseColorCommand("Player1", "RED"));

        verify(player1, times(1)).setColor(TotemColor.RED);
    }

    @Test
    @DisplayName("visit(ChooseColorCommand) should notify observers after choosing")
    void testChooseColorNotifiesNextPlayerTurn() throws Exception {
        colorChoosingPhase.onEnter();

        colorChoosingPhase.visit(new ChooseColorCommand("Player1", "RED"));

        assertNull(colorChoosingPhase.getCurrentPlayer());
        verify(gameModel, times(2)).notifyChange();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // visit(ChooseColorCommand) Tests - Error Cases
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("visit(ChooseColorCommand) any player can choose in any order (no turn enforcement)")
    void testChooseColorIgnoresWrongPlayer() throws Exception {
        colorChoosingPhase.onEnter();

        // No turn order enforcement: Player2 can choose before Player1
        assertDoesNotThrow(() -> colorChoosingPhase.visit(new ChooseColorCommand("Player2", "RED")));
        verify(player2, times(1)).setColor(TotemColor.RED);
    }

    @Test
    @DisplayName("visit(ChooseColorCommand) should throw when unavailable color is selected")
    void testChooseColorIgnoresUnavailableColor() throws Exception {
        colorChoosingPhase.onEnter();
        colorChoosingPhase.visit(new ChooseColorCommand("Player1", "RED"));

        assertThrows(IllegalArgumentException.class,
                () -> colorChoosingPhase.visit(new ChooseColorCommand("Player2", "RED")));

        verify(player2, never()).setColor(any());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Multiple Choice Sequence Tests
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("All players should be able to choose colors in sequence")
    void testSequentialColorChoiceForAllPlayers() throws Exception {
        colorChoosingPhase.onEnter();

        colorChoosingPhase.visit(new ChooseColorCommand("Player1", "RED"));
        colorChoosingPhase.visit(new ChooseColorCommand("Player2", "BLUE"));
        colorChoosingPhase.visit(new ChooseColorCommand("Player3", "PURPLE"));

        verify(player1, times(1)).setColor(TotemColor.RED);
        verify(player2, times(1)).setColor(TotemColor.BLUE);
        verify(player3, times(1)).setColor(TotemColor.PURPLE);
    }

    @Test
    @DisplayName("Phase should transition to SetupPhase after all players choose colors")
    void testPhaseTransitionToSetupPhaseAfterAllChoose() throws Exception {
        colorChoosingPhase.onEnter();

        colorChoosingPhase.visit(new ChooseColorCommand("Player1", "RED"));
        colorChoosingPhase.visit(new ChooseColorCommand("Player2", "BLUE"));
        colorChoosingPhase.visit(new ChooseColorCommand("Player3", "PURPLE"));

        verify(gameModel, times(1)).setPhase(any(SetupPhase.class));
    }

    @Test
    @DisplayName("Phase should not transition to SetupPhase before last player chooses")
    void testPhaseDoesNotTransitionBeforeLastChoice() throws Exception {
        colorChoosingPhase.onEnter();

        colorChoosingPhase.visit(new ChooseColorCommand("Player1", "RED"));
        verify(gameModel, never()).setPhase(any(SetupPhase.class));

        colorChoosingPhase.visit(new ChooseColorCommand("Player2", "BLUE"));
        verify(gameModel, never()).setPhase(any(SetupPhase.class));

        colorChoosingPhase.visit(new ChooseColorCommand("Player3", "PURPLE"));
        verify(gameModel, times(1)).setPhase(any(SetupPhase.class));
    }

    @Test
    @DisplayName("Should notify completion when all players finish choosing colors")
    void testNotifyCompletionWhenAllPlayersChoose() throws Exception {
        colorChoosingPhase.onEnter();

        colorChoosingPhase.visit(new ChooseColorCommand("Player1", "RED"));
        colorChoosingPhase.visit(new ChooseColorCommand("Player2", "BLUE"));
        colorChoosingPhase.visit(new ChooseColorCommand("Player3", "PURPLE"));

        verify(gameModel, times(1)).setPhase(any(SetupPhase.class));
        verify(gameModel, times(4)).notifyChange();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Max player test
    // ──────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Each color should only be available once across all players")
    void testColorUniquenessAcrossAllPlayers() throws Exception {
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

        when(localPlayer1.isConnected()).thenReturn(true);
        when(localPlayer2.isConnected()).thenReturn(true);
        when(localPlayer3.isConnected()).thenReturn(true);
        when(localPlayer4.isConnected()).thenReturn(true);
        when(localPlayer5.isConnected()).thenReturn(true);

        List<Player> localPlayers = List.of(localPlayer1, localPlayer2, localPlayer3, localPlayer4, localPlayer5);
        when(localModel.getPlayers()).thenReturn(localPlayers);
        when(localModel.getPlayerCount()).thenReturn(localPlayers.size());
        when(localModel.getPlayerByName("Player1")).thenReturn(localPlayer1);
        when(localModel.getPlayerByName("Player2")).thenReturn(localPlayer2);
        when(localModel.getPlayerByName("Player3")).thenReturn(localPlayer3);
        when(localModel.getPlayerByName("Player4")).thenReturn(localPlayer4);
        when(localModel.getPlayerByName("Player5")).thenReturn(localPlayer5);

        ColorChoosingPhase localPhase = new ColorChoosingPhase(localModel);
        localPhase.onEnter();

        localPhase.visit(new ChooseColorCommand("Player1", "RED"));
        localPhase.visit(new ChooseColorCommand("Player2", "BLUE"));
        localPhase.visit(new ChooseColorCommand("Player3", "PURPLE"));
        localPhase.visit(new ChooseColorCommand("Player4", "YELLOW"));

        assertThrows(IllegalArgumentException.class,
                () -> localPhase.visit(new ChooseColorCommand("Player5", "RED")));

        verify(localPlayer5, never()).setColor(TotemColor.RED);
    }
  

    @Test
    @DisplayName("Color choosing phase has no turn tracking: getCurrentPlayer() returns null")
    void testSkipPlayerWhenDisconnected() throws Exception {
        FakeVirtualView vv1 = new FakeVirtualView();
        FakeVirtualView vv2 = new FakeVirtualView();
        FakeVirtualView vv3 = new FakeVirtualView();

        GameModel localModel = new GameModel(List.of(vv1, vv2, vv3));
        List<String> localPlayers = List.of("Player1", "Player2", "Player3");
        localModel.startGame(localPlayers);

        Player pl2 = localModel.getPlayerByName("Player2");
        pl2.setDisconnected();

        assertEquals(GamePhase.COLOR_CHOOSING_PHASE, localModel.getCurrentPhase());

        // No turn tracking: getCurrentPlayer() returns null during color choosing
        assertNull(localModel.getCurrentPlayer());

        // Any player can choose in any order
        localModel.handleCommand(new ChooseColorCommand("Player1", "RED"));
        assertEquals(TotemColor.RED, localModel.getPlayerByName("Player1").getColor());

        // Phase stays in color choosing until all players have chosen
        assertEquals(GamePhase.COLOR_CHOOSING_PHASE, localModel.getCurrentPhase());
    }

}
