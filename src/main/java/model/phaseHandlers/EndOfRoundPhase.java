package model.phaseHandlers;

import model.GameModel;
import model.enums.GamePhase;
import model.player.Player;

public class EndOfRoundPhase extends GamePhaseHandler {

    public EndOfRoundPhase(GameModel model) {
        super(model);
    }

    @Override
    public void onEnter(){
        model.getBoard().resolveEvents(model.getPlayers()); // TODO è giusto che sia il board (bottom row) a risovere gli eventi
        model.notifyChange("events_resolved");

        model.getBoard().endRound();

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
