package model.phaseHandlers;

import model.GameModel;
import model.enums.Era;
import model.enums.GamePhase;
import model.player.Player;
import model.rowsManager.RowsManager;
import network.server.core.VirtualView;
import shared.dto.event.EventResolutionDto;

import java.util.List;

public class EndOfRoundPhase implements GamePhaseHandler {

    private final GameModel model;

    public EndOfRoundPhase(GameModel model) {
        this.model = model;
    }

    /**
     * @implNote This method resolves all events on the board, broadcasts one
     * {@code EventResolvedMessage} per resolved event card (so the client can
     * show an explanatory screen), checks for era changes, and transitions
     * to the next phase (either PlacementPhase or EndOfGamePhase) based on
     * whether the game is over.
     */
    @Override
    public void onEnter() {
        RowsManager rowsManager = model.getRowsManager();
        List<EventResolutionDto> resolutions = rowsManager.resolveEvents(model.getPlayers());
        for (EventResolutionDto r : resolutions) {
            for (VirtualView v : model.getViews()) {
                v.sendEventResolved(r);
            }
        }
        // model.notifyChange();

        model.incrementRound();

        if (model.isGameOver()) {
            model.setPhase(new EndOfGamePhase(model, null, false));
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
