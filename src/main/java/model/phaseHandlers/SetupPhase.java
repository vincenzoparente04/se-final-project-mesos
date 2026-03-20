package model.phaseHandlers;

import model.GameModel;
import model.enums.GamePhase;
import model.player.Player;

import java.util.List;

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

    /**
     * Gives each player the starting amount of food based on their position in the turn order, as determined by the TurnOrderTile.
     */
    private void distributeStartingResources() {
        List<Player> players = model.getBoard().getTurnOrderTile().getTurnOrder();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            switch (i) {
                case 0 -> p.addFood(2); //player 1
                case 1, 2 -> p.addFood(3); //player 2 and 3
                case 3, 4 -> p.addFood(4); //player 4 and 5
            }
        }
    }

        @Override
        public GamePhase getPhase () {
            return model.getCurrentPhase();
        }

        @Override
        public Player getCurrentPlayer () {
            return model.getCurrentPlayer();
        }

}
