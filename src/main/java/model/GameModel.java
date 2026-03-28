package model;

import model.board.Board;
import model.enums.Era;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.phaseHandlers.ColorChoosingPhase;
import model.phaseHandlers.GamePhaseHandler;
import model.player.Player;
import model.rowsManager.RowsManager;

import java.util.ArrayList;
import java.util.List;

public class GameModel {
    private Board board;
    private RowsManager rowsManager;
    private List<Player> players;
    private int playerCount;
    private int currentRound;
    private Era currentEra;

    private GamePhaseHandler currentPhaseHandler;

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

    public void setPhase(GamePhaseHandler phase) {
        this.currentPhaseHandler = phase;
        //notifyChange("phase_changed:" + phase.getPhase());
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

    public GamePhase getCurrentPhase() {
        return currentPhaseHandler != null ? currentPhaseHandler.getPhase() : null;
    }

    public Player getCurrentPlayer() {
        return currentPhaseHandler != null ? currentPhaseHandler.getCurrentPlayer() : null;
    }

    public List<Player> getTurnOrder() {
        return board.getTurnOrderTile().getTurnOrder();
    }

    /**
     * @implNote the current era is determined by the current era of the tribe deck, which is updated at each tribe deck draw.<br>
     * <p><b>NOTE: </b><br><u>this.currentEra</u> is updated only when this method is called. So the real updated current era is stored in the tribe dech</p>
     * @return the current era
     */
    public Era getCurrentEra() {
        currentEra = rowsManager.getTribeDeck().getCurrentEra();
        return currentEra;
    }


    // helpers:
    public boolean isGameOver() {
        return currentRound > 10;
    }

    public void incrementRound() {
        currentRound++;
    }
}