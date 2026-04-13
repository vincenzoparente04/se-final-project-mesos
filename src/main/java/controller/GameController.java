package controller;

import model.GameModel;
import model.enums.TotemColor;
import model.player.Player;
import shared.dto.GameStateDto;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mediator between the domain model and the outside world.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Translates raw network parameters (strings, primitives) into model calls.</li>
 *   <li>Registers internally on {@link GameModel} via {@code PropertyChangeSupport} and,
 *       on every change, builds a {@link GameStateDto} and forwards it to all
 *       registered DTO listeners.</li>
 *   <li>Never imports or references any network/server class (VirtualView, Socket, etc.).</li>
 * </ul>
 */
public class GameController {

    private final GameModel gameModel;
    private final List<Consumer<GameStateDto>> dtoListeners = new ArrayList<>();

    public GameController(GameModel gameModel) {
        this.gameModel = gameModel;
        gameModel.addPropertyChangeListener(evt -> broadcastDto());
    }

    // ─────────────────────────────────────────────────────────
    // DTO listener registration
    // ─────────────────────────────────────────────────────────

    /**
     * Registers a listener that receives a freshly built {@link GameStateDto}
     * every time the model state changes.
     * The listener is invoked on the same thread that triggered the model change.
     */
    public void addDtoListener(Consumer<GameStateDto> listener) {
        dtoListeners.add(listener);
    }

    // ─────────────────────────────────────────────────────────
    // Public API — all parameters are primitives or strings
    // (no domain objects cross this boundary)
    // ─────────────────────────────────────────────────────────

    public synchronized void startGame(List<String> playerNames) {
        gameModel.startGame(playerNames);
    }

    /**
     * Called by the client to select a totem color during the color-choosing phase.
     * @param playerName the name of the player making the choice
     * @param colorName  the chosen color as a string (case-insensitive, e.g. "RED")
     */
    public synchronized void chooseColor(String playerName, String colorName) {
        Player player = resolveCurrentPlayer(playerName);
        TotemColor color = TotemColor.valueOf(colorName.toUpperCase());
        gameModel.chooseColor(player, color);
    }

    /**
     * Called by the client to place a totem on an offer tile during the placement phase.
     * @param playerName the name of the player placing the totem
     * @param tileId     the letter identifier of the target offer tile (e.g. 'A')
     */
    public synchronized void placeTotem(String playerName, char tileId) {
        Player player = resolveCurrentPlayer(playerName);
        gameModel.placeTotem(player, tileId);
    }

    /**
     * Called by the client to draw a card from the board during the action phase.
     * @param playerName the name of the player drawing the card
     * @param cardId     the ID of the card to draw
     */
    public synchronized void drawCard(String playerName, int cardId) throws Exception {
        resolveCurrentPlayer(playerName);
        gameModel.drawCard(cardId);
    }

    /**
     * Called by the client to end the current turn.
     * @param playerName the name of the player ending their turn
     */
    public synchronized void endTurn(String playerName) {
        resolveCurrentPlayer(playerName);
        gameModel.endTurn();
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    /**
     * Validates that it is the given player's turn and returns their Player object.
     * Throws if the player does not exist or if it is not their turn.
     */
    private Player resolveCurrentPlayer(String playerName) {
        Player requested = gameModel.getPlayerByName(playerName);
        Player current = gameModel.getCurrentPlayer();
        if (current == null || !current.getName().equals(playerName)) {
            throw new IllegalStateException("It is not " + playerName + "'s turn.");
        }
        return requested;
    }

    private void broadcastDto() {
        GameStateDto dto = GameStateDtoBuilder.build(gameModel);
        dtoListeners.forEach(l -> l.accept(dto));
    }
}
