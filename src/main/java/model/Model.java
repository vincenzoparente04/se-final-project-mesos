package model;

import model.board.Board;
import model.board.OfferTile;
import model.enums.Era;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.player.Player;
import model.player.Totem;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private Board board;
    private List<Player> players;
    private int playerCount;
    private int currentRound;
    private Era currentEra;

    private GamePhaseHandler currentPhaseHandler;

    public void startGame(List<String> playerNames) {
        createPlayers(playerNames);
        setPhase(new ColorChoosingPhase(this));

    }

    /**
     * Generic action entry point. The controller calls this for every player action;
     * the current phase handler decides what to do with it.
     *
     * @param player the player performing the action
     * @param target phase-dependent payload (TotemColor, OfferTile, cardId …)
     */
    public void chooseColor(Player player, TotemColor totemColor) throws Exception {
        currentPhaseHandler.chooseColor(player, totemColor);
    }

   public void placeTotem(Player player, OfferTile offerTile) throws Exception {
        currentPhaseHandler.placeTotem(player, offerTile);
   }

   public void drawCard(int cardId) throws Exception {
        currentPhaseHandler.drawCard(cardId);
   }

    public void setPhase(GamePhaseHandler phase) {
        this.currentPhaseHandler = phase;
        notifyChange("phase_changed:" + phase.getPhase());
        phase.onEnter();
    }

    private void createPlayers(List<String> playerNames){
        this.players = new ArrayList<>();
        for (String name : playerNames) {
            this.players.add(new Player(name));
            this.playerCount++;
        }
    }

    // ── getters ──────────────────────────────────────────────────────────────────

    public Board getBoard()                     { return board; }
    public List<Player> getPlayers()            { return players; }
    public int getPlayerCount()                 { return playerCount; }
    public int getCurrentRound()                { return currentRound; }
    public Era getCurrentEra()                  { return currentEra; }
    public GamePhaseHandler getPhaseHandler()   { return currentPhaseHandler; }

    public GamePhase getCurrentPhase() {
        return currentPhaseHandler != null ? currentPhaseHandler.getPhase() : null;
    }

    public Player getCurrentPlayer() {
        return currentPhaseHandler != null ? currentPhaseHandler.getCurrentPlayer() : null;
    }



    public boolean isGameOver() {
        return currentRound > 10 || board.isTribeDeckEmpty();
    }

    public void incrementRound() {
        currentRound++;
    }
}