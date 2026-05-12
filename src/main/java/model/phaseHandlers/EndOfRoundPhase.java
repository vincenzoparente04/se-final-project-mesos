package model.phaseHandlers;

import model.GameModel;
import model.enums.Era;
import model.enums.GamePhase;
import model.player.Player;
import model.rowsManager.RowsManager;

public class EndOfRoundPhase implements GamePhaseHandler {

    private final GameModel model;

    public EndOfRoundPhase(GameModel model) {
        this.model = model;
    }

    /**
     * @implNote This method resolves all events on the board, checks for era changes, and transitions
     * to the next phase (either PlacementPhase or EndOfGamePhase) based on whether the game is over.
     * It also notifies observers of any changes that occur during this process.
     */
    @Override
    public void onEnter() {
        RowsManager rowsManager = model.getRowsManager();
        rowsManager.resolveEvents(model.getPlayers());
        model.notifyChange();

        model.incrementRound();

        if (model.isGameOver()) {
            model.setPhase(new EndOfGamePhase(model));
            return;
        }

        Era currentEra = model.getCurrentEra();
        rowsManager.endRound(model.getPlayerCount());

        if (model.getCurrentEra().compareTo(currentEra) != 0) {
            model.notifyChange();
            rowsManager.changeEra();
        }

        model.setPhase(new PlacementPhase(model));
    }

    @Override
    public GamePhase getPhase() {
        return GamePhase.END_OF_ROUND;
    }

    @Override
    public Player getCurrentPlayer() {
        return null;
    }
}
