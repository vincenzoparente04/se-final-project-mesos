package controller;

import model.GameModel;
import model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shared.command.ChooseColorCommand;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.GameCommand;
import shared.command.PlaceTotemCommand;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GameController Tests")
class GameControllerTest {

    private GameModel model;
    private GameController controller;

    private Player currentPlayer;

    @BeforeEach
    void setUp() {
        model = mock(GameModel.class);
        controller = new GameController(model);

        currentPlayer = mock(Player.class);
        when(currentPlayer.getName()).thenReturn("Alice");
        when(model.getCurrentPlayer()).thenReturn(currentPlayer);
    }

    // ── startGame ─────────────────────────────────────────────

    @Test
    @DisplayName("startGame delegates player names to the model")
    void startGameDelegatesToModel() {
        List<String> names = List.of("Alice", "Bob");

        controller.startGame(names);

        verify(model).startGame(names);
    }

    // ── handleCommand — turn validation ───────────────────────

    @Test
    @DisplayName("handleCommand delegates to model when it is the player's turn")
    void handleCommandDelegatesToModel() throws Exception {
        GameCommand cmd = new ChooseColorCommand("Alice", "RED");

        controller.handleCommand(cmd);

        verify(model).handleCommand(cmd);
    }

    @Test
    @DisplayName("handleCommand throws when current player is null")
    void handleCommandThrowsWhenCurrentPlayerNull() throws Exception {
        when(model.getCurrentPlayer()).thenReturn(null);
        GameCommand cmd = new ChooseColorCommand("Alice", "RED");

        assertThrows(IllegalStateException.class, () -> controller.handleCommand(cmd));

        verify(model, never()).handleCommand(cmd);
    }

    @Test
    @DisplayName("handleCommand throws when it is not the player's turn")
    void handleCommandThrowsWhenWrongTurn() throws Exception {
        Player other = mock(Player.class);
        when(other.getName()).thenReturn("Bob");
        when(model.getCurrentPlayer()).thenReturn(other);

        GameCommand cmd = new ChooseColorCommand("Alice", "RED");

        assertThrows(IllegalStateException.class, () -> controller.handleCommand(cmd));

        verify(model, never()).handleCommand(cmd);
    }

    @Test
    @DisplayName("handleCommand works for PlaceTotemCommand")
    void handleCommandPlaceTotem() throws Exception {
        GameCommand cmd = new PlaceTotemCommand("Alice", 'A');

        controller.handleCommand(cmd);

        verify(model).handleCommand(cmd);
    }

    @Test
    @DisplayName("handleCommand works for DrawCardCommand")
    void handleCommandDrawCard() throws Exception {
        GameCommand cmd = new DrawCardCommand("Alice", 42);

        controller.handleCommand(cmd);

        verify(model).handleCommand(cmd);
    }

    @Test
    @DisplayName("handleCommand works for EndTurnCommand")
    void handleCommandEndTurn() throws Exception {
        GameCommand cmd = new EndTurnCommand("Alice");

        controller.handleCommand(cmd);

        verify(model).handleCommand(cmd);
    }
}
