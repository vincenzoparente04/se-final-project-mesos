package model.phaseHandlers;

import model.GameModel;
import model.enums.GamePhase;
import model.player.Player;

public class SetupPhase extends GamePhaseHandler {

    public SetupPhase(GameModel model) {
        super(model);
    }

    @Override
    public void onEnter() {
        // Board owns the decks and knows how to set itself up
        model.getBoard().setup(model.getPlayerCount());

        // Randomize turn order: board places totems on the TurnOrderTile
        model.getBoard().randomizeTurnOrder(model.getPlayers());

        // Give each player their starting food
        distributeStartingResources();

        // Done — move to placement
        model.setPhase(new PlacementPhase(model));
    }

    private void distributeStartingResources() {
        for (Player player : model.getPlayers()) {
            player.addFood(3);  // TODO: confirm starting food amount from rules
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
