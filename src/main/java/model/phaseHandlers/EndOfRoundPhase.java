package model.phaseHandlers;

import model.GameModel;

public class EndOfRoundPhase implements GamePhaseHandler {
    private final GameModel model;

    public EndOfRoundPhase(GameModel model) {
        this.model = model;
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
}
