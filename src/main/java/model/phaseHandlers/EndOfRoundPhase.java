package model.phaseHandlers;

import model.GameModel;
import model.enums.GamePhase;
import model.player.Player;

public class EndOfRoundPhase extends GamePhaseHandler {

    public EndOfRoundPhase(GameModel model) {
        super(model);
    }

    /**
     * @implNote in this phase we resolve the events present in the bottom row,
     * then we call the endRound method of the board to discard the tribe cards and add new ones,
     * then we check if the game is over (if a player has 12 or more points), if it is we set the phase to EndOfGamePhase, otherwise we increment the round and set the phase to PlacementPhase
     */
    @Override
    public void onEnter(){
        model.getBoard().resolveEvents(model.getPlayers());
        model.notifyChange("events_resolved");

        int currentEra = model.getCurrentEra();

        model.getBoard().endRound(model.getPlayerCount());

        if (currentEra != model.getCurrentEra()) {
            model.notifyChange("era_changed:" + model.getCurrentEra());
            model.getBoard().changeEra();
        }

        if (model.isGameOver()) {
            model.setPhase(new EndOfGamePhase(model));
        } else {
            model.incrementRound();
            model.setPhase(new PlacementPhase(model));
        }
    }

    @Override
    public GamePhase getPhase() {
        return model.getCurrentPhase();
    }

    @Override
    public Player getCurrentPlayer() {
        return model.getCurrentPlayer();
    }
}
