package model;

import javafx.beans.Observable;
import model.board.Board;
import model.deck.BuildingDeck;
import model.deck.TribeDeck;
import model.enums.Era;
import model.enums.GamePhase;
import model.enums.GameState;
import model.player.Player;

import java.util.List;

public class GameModel extends Observable {

    // board components
    private final Board board;
    private TribeDeck tribeDeck;
    private final BuildingDeck buildingDeckEraI;
    private final BuildingDeck buildingDeckEraII;
    private final BuildingDeck buildingDeckEraIII;

    // players
    private final List<Player> players;
    // fixed order: the turn order is managed by the TurnOrderTile

    // game state
    private int currentRound;       // 1-10
    private Era currentEra;         // ERA_I, ERA_II, ERA_III
    private GamePhase currentPhase; // vedi enum sotto
    private GameState gameState;    // SETUP, RUNNING, ENDED

    // getter for components
    public Board getBoard()
    public TribeDeck getTribeDeck()
    public BuildingDeck getBuildingDeck(Era era)

    // getter for players
    public List<Player> getPlayers()
    //public Player getPlayerByColor(PlayerColor color) // REFACTOR
    public int getPlayerCount()

    // getter for game state

    public int getCurrentRound()
    public Era getCurrentEra()
    public GamePhase getCurrentPhase()
    public GameState getGameState()

    // state setters
    // only called by the Controller to update the game state after applying the game logic, never directly by the View
    public void setCurrentRound(int round)
    public void setCurrentEra(Era era)
    public void setCurrentPhase(GamePhase phase)
    public void setGameState(GameState state)

    // Observer
    public void notifyChange(String changeType)
    // calls setChanged() + notifyObservers(changeType)
    // i Controller chiamano questo dopo ogni modifica significativa
}