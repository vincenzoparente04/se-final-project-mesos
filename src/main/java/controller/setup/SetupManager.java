package controller.setup;

import model.GameModel;
import java.util.List;

// SetupManager: responsible for initializing the game state at the beginning of the game, called by the GameController during startGame()
public class SetupManager {

    private final GameModel model;
    private final GameView view;

    public void setup(List<String> playerNames)
    // ccoordinates the setup phase, called by the GameController during startGame()
    // 1. creates the players based on the playerNames and adds them to the model
    // 2. builds the TribeDeck and the BuildingDecks
    // 3. populates the TopRow and the BottomRow with the initial cards
    // 4. distributes the initial Food
    // 5. places the Totems on the TurnOrderTile in random order


    // methods used in setup():
    private void createPlayers(List<String> playerNames)
    // creates a Player for each name and adds them to the model

    private void buildTribeDeck()
    // builds the TribeDeck by adding all the TribeCard in the correct order:
    // filters the cards based on the number of players (some cards are only used in games with 3 or 4 players),
    // then adds the Era I cards, then the Era II cards, then the Era III cards, and finally the Final Event cards at the bottom

    private void buildBuildingDecks()
    // builds the BuildingDecks by adding all the BuildingCard in the correct order:

    private void populateInitialRows()
    // populates BottomRow: draw playerCount + 1 TribeCard
    //   (if an EventCard, goes in TopRow)
    // populates TopRow: draw playerCount + 4 TribeCard
    //   + adds the BuildingCard of Era I

    private void distributeInitialFood()
    // each player starts with 5 Food, except the last player in the turn order, who starts with 4 Food

    private void randomizeTurnOrder()
    // places the Totems on the TurnOrderTile in random order to determine the turn order for the first round
}
