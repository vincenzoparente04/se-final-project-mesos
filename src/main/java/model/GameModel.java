package model;

import model.board.Board;
import model.enums.Era;
import model.enums.GamePhase;
import model.phaseHandlers.ColorChoosingPhase;
import model.phaseHandlers.GamePhaseHandler;
import model.player.Player;
import model.rowsManager.RowsManager;
import network.server.core.VirtualView;
import shared.command.GameCommand;
import shared.dto.GameStateDto;

import java.util.ArrayList;
import java.util.List;

public class GameModel {
    private static final int MAX_ROUNDS = 10;

    private final Board board;
    private final RowsManager rowsManager = new RowsManager();
    private List<Player> players = new ArrayList<>();
    private int currentRound = 1;
    private List<String> winners = new ArrayList<>();

    private GamePhaseHandler currentPhaseHandler;
    private final List<VirtualView> views;

    public GameModel(List<VirtualView> views) {
        this.views = new ArrayList<>(views);
        this.board = new Board(views.size());
    }


    public void startGame(List<String> playerNames) {
        createPlayers(playerNames);
        setPhase(new ColorChoosingPhase(this));
    }

    private void createPlayers(List<String> playerNames) {
        this.players = new ArrayList<>();
        for (String name : playerNames) {
            this.players.add(new Player(name));
        }
    }

    public void setPhase(GamePhaseHandler phase) {
        this.currentPhaseHandler = phase;
        phase.onEnter();
    }

    /**
     * Single dispatch point for all in-game commands. Snapshots the current
     * phase handler once and visits the command on it: the handler decides,
     * via polymorphic dispatch on the command type, whether and how to
     * execute it.
     */
    public void handleCommand(GameCommand cmd) throws Exception {
        GamePhaseHandler handler = this.currentPhaseHandler;
        cmd.accept(handler);
    }

    public void notifyChange() {
        GameStateDto dto = GameStateDtoBuilder.build(this);
        views.forEach(v -> v.sendState(dto));
    }

    // On player reconnection:     // TODO TIENINE UNO SOLO
    public void addView(VirtualView view) {
        views.add(view);
    }

    public synchronized void swapView(String playerName, VirtualView newView) {
        views.removeIf(v -> v.getPlayerName().equals(playerName));
        views.add(newView);
    }

    public void incrementRound() {
        currentRound++;
    }

    public boolean isGameOver() {
        return currentRound > MAX_ROUNDS;
    }

    public void setWinners(List<String> winnerNames) {
        this.winners = winnerNames;
    }

    // getters –––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––
    public Board getBoard() {
        return board;
    }

    public RowsManager getRowsManager(){
        return rowsManager;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getPlayerCount() {
        return players.size(); // TODO mettere magari un filtro che conta solo active players
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public GamePhaseHandler getPhaseHandler() {
        return currentPhaseHandler;
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

    public Player getPlayerByName(String name) {
        return players.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No player named: " + name));
    }

    public List<String> getWinners() {
        return winners;
    }
}
