package controller;

import model.GameModel;
import java.util.List;

// GameController: pure orchestrator, no game logic, only calls methods in the correct order
public class GameController {
    private final GameModel model;
    private final GameView view;

    private final SetupManager setupManager;
    private final RoundManager roundManager;
    private final EndOfRoundManager endOfRoundManager;
    private final EndOfGameManager endOfGameManager;

    // constructor with model and view
    public GameController(GameModel model, GameView view)

    public void startGame(List<String> playerNames)
    // starting point of the game, called by the main method after creating the model and the view
    // 1. calls setupManager.setupGame(playerNames) to initialize the game state
    // 2. launches the game loop

    private void gameLoop(){
        // repeats for 10 rounds:
        //   roundManager.playRound()
        //   endOfRoundManager.resolveEndOfRound()
        //   if isGameOver() break
        // then endOfGameManager.resolveEndOfGame()
        while (!isGameOver()) {
            roundManager.playRound();
            endOfRoundManager.resolveEndOfRound();
            model.setCurrentRound(model.getCurrentRound() + 1);
        }
        endOfGameManager.resolveEndOfGame();
    }

    private boolean isGameOver()
    // true if currentRound == 10 || tribeDeck.isEmpty()
}