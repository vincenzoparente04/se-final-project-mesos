package model;

import model.board.Board;
import model.enums.Era;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.phaseHandlers.ColorChoosingPhase;
import model.phaseHandlers.GamePhaseHandler;
import model.player.Player;
import model.rowsManager.RowsManager;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public class GameModel {
    private static final int MAX_ROUNDS = 10;

    private Board board = new Board();
    private RowsManager rowsManager = new RowsManager();
    private List<Player> players = new ArrayList<>();
    private int playerCount;
    private int currentRound = 1;
    private List<String> winners = new ArrayList<>();

    private GamePhaseHandler currentPhaseHandler;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void startGame(List<String> playerNames) {
        createPlayers(playerNames);
        setPhase(new ColorChoosingPhase(this));
    }

    public void chooseColor(Player player, TotemColor totemColor) {
        currentPhaseHandler.chooseColor(player, totemColor);
    }

   public void placeTotem(Player player, char tileId) {
        currentPhaseHandler.placeTotem(player, tileId);
   }

   public void drawCard(int cardId) throws Exception {
        currentPhaseHandler.drawCard(cardId);
   }

   public void endTurn() {
        currentPhaseHandler.endTurn();
   }

    public void setPhase(GamePhaseHandler phase) {
        this.currentPhaseHandler = phase;
        phase.onEnter();
    }

    private void createPlayers(List<String> playerNames){
        this.players = new ArrayList<>();
        for (String name : playerNames) {
            this.players.add(new Player(name));
            this.playerCount++;
        }
    }

    // -- getters --
    public Board getBoard()                     { return board; }
    public RowsManager getRowsManager()         { return rowsManager; }
    public List<Player> getPlayers()            { return players; }
    public int getPlayerCount()                 { return playerCount; }
    public int getCurrentRound()                { return currentRound; }
    public GamePhaseHandler getPhaseHandler()   { return currentPhaseHandler; }

    /**
     * Resolves a Player by name. Throws if no player with that name exists.
     */
    public Player getPlayerByName(String name) {
        return players.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No player named: " + name));
    }

    public GamePhase getCurrentPhase() {
        return currentPhaseHandler != null ? currentPhaseHandler.getPhase() : null;
    }

    public Player getCurrentPlayer() {
        return currentPhaseHandler != null ? currentPhaseHandler.getCurrentPlayer() : null;
    }

    public List<Player> getTurnOrder() {
        return board.getTurnOrder();
    }

    public Era getCurrentEra() {
        return rowsManager.getCurrentEra();
    }


    // helpers:
    public boolean isGameOver() {
        return currentRound > MAX_ROUNDS;
    }

    public void incrementRound() {
        currentRound++;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void setWinners(List<String> winnerNames) {
        this.winners = winnerNames;
    }

    public List<String> getWinners() {
        return winners;
    }

    public void notifyChange(String message) {
        pcs.firePropertyChange("gameState", null, message);
    }
}