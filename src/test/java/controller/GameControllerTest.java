package controller;

import model.GameModel;
import model.enums.TotemColor;
import model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        when(model.getPlayerByName("Alice")).thenReturn(currentPlayer);
    }

    // ── startGame ─────────────────────────────────────────────

    @Test
    @DisplayName("startGame delegates player names to the model")
    void startGameDelegatesToModel() {
        List<String> names = List.of("Alice", "Bob");

        controller.startGame(names);

        verify(model).startGame(names);
    }

    // ── chooseColor ───────────────────────────────────────────

    @Test
    @DisplayName("chooseColor delegates to model with resolved player and parsed color")
    void chooseColorDelegatesToModel() {
        controller.chooseColor("Alice", "RED");

        verify(model).chooseColor(currentPlayer, TotemColor.RED);
    }

    @Test
    @DisplayName("chooseColor accepts color names case-insensitively")
    void chooseColorIsCaseInsensitive() {
        controller.chooseColor("Alice", "blue");

        verify(model).chooseColor(currentPlayer, TotemColor.BLUE);
    }

    @Test
    @DisplayName("chooseColor throws when it is not the player's turn")
    void chooseColorThrowsWhenWrongTurn() {
        when(model.getCurrentPlayer()).thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> controller.chooseColor("Alice", "RED"));

        verify(model, never()).chooseColor(currentPlayer, TotemColor.RED);
    }

    @Test
    @DisplayName("chooseColor throws for an unknown color name")
    void chooseColorThrowsForUnknownColor() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.chooseColor("Alice", "PURPLE"));
    }

    // ── placeTotem ────────────────────────────────────────────

    @Test
    @DisplayName("placeTotem delegates to model with resolved player and tile id")
    void placeTotemDelegatesToModel() {
        controller.placeTotem("Alice", 'A');

        verify(model).placeTotem(currentPlayer, 'A');
    }

    @Test
    @DisplayName("placeTotem throws when it is not the player's turn")
    void placeTotemThrowsWhenWrongTurn() {
        when(model.getCurrentPlayer()).thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> controller.placeTotem("Alice", 'A'));

        verify(model, never()).placeTotem(currentPlayer, 'A');
    }

    // ── drawCard ──────────────────────────────────────────────

    @Test
    @DisplayName("drawCard delegates to model")
    void drawCardDelegatesToModel() throws Exception {
        controller.drawCard("Alice", 42);

        verify(model).drawCard(42);
    }

    @Test
    @DisplayName("drawCard throws when it is not the player's turn")
    void drawCardThrowsWhenWrongTurn() {
        when(model.getCurrentPlayer()).thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> controller.drawCard("Alice", 42));
    }

    // ── endTurn ───────────────────────────────────────────────

    @Test
    @DisplayName("endTurn delegates to model")
    void endTurnDelegatesToModel() {
        controller.endTurn("Alice");

        verify(model).endTurn();
    }

    @Test
    @DisplayName("endTurn throws when it is not the player's turn")
    void endTurnThrowsWhenWrongTurn() {
        Player otherPlayer = mock(Player.class);
        when(otherPlayer.getName()).thenReturn("Bob");
        when(model.getCurrentPlayer()).thenReturn(otherPlayer);

        assertThrows(IllegalStateException.class,
                () -> controller.endTurn("Alice"));

        verify(model, never()).endTurn();
    }
}
