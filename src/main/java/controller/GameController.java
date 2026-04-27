package controller;

import model.GameModel;
import model.enums.TotemColor;
import model.player.Player;

import java.util.List;

public class GameController {

    private final GameModel gameModel;

    public GameController(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    public synchronized void startGame(List<String> playerNames) {
        gameModel.startGame(playerNames);
    }

    public synchronized void chooseColor(String playerName, String colorName) {
        Player player = resolveCurrentPlayer(playerName);
        TotemColor color = TotemColor.valueOf(colorName.toUpperCase());
        gameModel.chooseColor(player, color);
    }

    public synchronized void placeTotem(String playerName, char tileId) {
        Player player = resolveCurrentPlayer(playerName);
        gameModel.placeTotem(player, tileId);
    }

    public synchronized void drawCard(String playerName, int cardId) throws Exception {
        resolveCurrentPlayer(playerName);
        gameModel.drawCard(cardId);
    }

    public synchronized void endTurn(String playerName) {
        resolveCurrentPlayer(playerName);
        gameModel.endTurn();
    }

    private Player resolveCurrentPlayer(String playerName) {
        Player requested = gameModel.getPlayerByName(playerName);
        Player current = gameModel.getCurrentPlayer();
        if (current == null || !current.getName().equals(playerName)) {
            throw new IllegalStateException("It is not " + playerName + "'s turn.");
        }
        return requested;
    }
}
