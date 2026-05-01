package model;

import model.board.Board;
import model.enums.Era;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.phaseHandlers.ColorChoosingPhase;
import model.phaseHandlers.GamePhaseHandler;
import model.player.Player;
import model.rowsManager.RowsManager;
import network.server.core.VirtualView;
import shared.dto.GameStateDto;

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
    private final List<VirtualView> views;

    public GameModel(List<VirtualView> views) {
        // NB: NON copiare. Manteniamo il riferimento alla stessa lista che Game possiede,
        // così quando Game.onPlayerReconnected sostituisce la VirtualView del player
        // (rimosso V_old, aggiunto V_new), notifyChange() vede automaticamente la nuova view.
        // È responsabilità del chiamante passare una lista thread-safe (CopyOnWriteArrayList).
        this.views = views;    }

    public GameModel() {
        this.views = List.of();
    }

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
        currentPhaseHandler.drawCard(cardId); //todo
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

    public boolean isGameOver() {
        return currentRound > MAX_ROUNDS;
    }

    public void incrementRound() {
        currentRound++;
    }

    public void setWinners(List<String> winnerNames) {
        this.winners = winnerNames;
    }

    public List<String> getWinners() {
        return winners;
    }

    public void notifyChange() {
        GameStateDto dto = GameStateDtoBuilder.build(this);
        views.forEach(v -> v.sendState(dto));
    }
}
