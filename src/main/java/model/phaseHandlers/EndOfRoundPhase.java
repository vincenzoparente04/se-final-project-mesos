package model.phaseHandlers;

import model.GameModel;
import model.enums.Era;
import model.enums.GamePhase;
import model.player.Player;
import model.rowsManager.RowsManager;
import network.server.core.VirtualView;
import shared.dto.event.EventResolutionDto;

import java.util.List;

/**
 * Represents the transitional phase executed at the end of each game round,
 * responsible for standard event resolution and phase branching.
 * Upon activation, this handler triggers the routine event resolution logic,
 * broadcasting individual updates to all connected virtual views to allow clients
 * to render intermediate resolution states. Following resolution, it increments
 * the global round counter and evaluates the match termination conditions.
 * <p>
 * This phase determines the next state of the game by branching into one of two paths:
 * <ul>
 * <li><b>Match Continuation:</b> If terminal conditions are not met, checks for era transitions (notifying observers upon change),
 * and routes the state machine into the next {@code PlacementPhase}.</li>
 * <li><b>Match Termination:</b> If the game is flagged as over, it immediately triggers
 * a transition into a standard {@code EndOfGamePhase}.</li>
 * </ul>
 * @see GamePhaseHandler
 * @see model.rowsManager.RowsManager
 * @see model.phaseHandlers.PlacementPhase
 * @see model.phaseHandlers.EndOfGamePhase
 */
public class EndOfRoundPhase implements GamePhaseHandler {

    private final GameModel model;

    public EndOfRoundPhase(GameModel model) {
        this.model = model;
    }

    /**
     * This method resolves all events on the board, broadcasts one
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
