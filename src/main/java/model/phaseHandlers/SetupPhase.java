package model.phaseHandlers;

import model.GameModel;
import model.board.TurnOrderSlot;
import model.enums.GamePhase;
import model.enums.TotemLocation;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

public class SetupPhase implements GamePhaseHandler {

    private final GameModel model;

    public SetupPhase(GameModel model) {
        this.model = model;
    }

    /**
     *   This method is responsible for setting up the board, randomizing turn order, and distributing
     * starting resources to players. Once all setup tasks are complete, it transitions the game to the PlacementPhase.
     */
    @Override
    public void onEnter() {
        model.getRowsManager().setup(model.getPlayerCount());
        randomizeTurnOrder(model.getPlayers());
        distributeStartingResources();

        model.setPhase(new PlacementPhase(model));
    }

    /**
     *  Randomizes the turn order by shuffling the list of players and placing their totems on the
     * TurnOrderTile in the new order. Each player is also set to be located on the TurnOrderTile.
     */
    public void randomizeTurnOrder(List<Player> players) {
        List<TurnOrderSlot> slots = model.getBoard().getTurnOrderTile().getSlots();

        List<Player> randomized = new ArrayList<>(players);
        java.util.Collections.shuffle(randomized);

        for (int i = 0; i < randomized.size(); i++) {
            Player p = randomized.get(i);
            slots.get(i).placeTotem(p);
            p.setLocation(TotemLocation.TURN_ORDER_TILE);
        }
    }

    /**
     *  Distributes starting resources to players based on their position in the turn order tile.
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
    public GamePhase getPhase() {
        return GamePhase.SETUP;
    }

    @Override
    public Player getCurrentPlayer() {
        return null;
    }
}
