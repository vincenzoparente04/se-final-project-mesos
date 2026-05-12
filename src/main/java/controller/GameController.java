package controller;

import model.GameModel;
import model.player.Player;
import shared.command.GameCommand;

import java.util.List;

public class GameController {

    private final GameModel gameModel;

    public GameController(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    public synchronized void startGame(List<String> playerNames) {
        gameModel.startGame(playerNames);
    }

    /**
     * Single entry point for in-game commands. Validates that the command's
     * player is the current player, then delegates dispatch to the model.
     * Cross-cutting validation that was duplicated in the old per-command
     * methods now lives here once.
     */
    public synchronized void handleCommand(GameCommand cmd) throws Exception {
        Player current = gameModel.getCurrentPlayer();
        if
        (current == null || !current.getName().equals(cmd.getPlayerName())) {
            throw new IllegalStateException("It is not " + cmd.getPlayerName() + "'s turn.");
        }
        gameModel.handleCommand(cmd);
    }
}
