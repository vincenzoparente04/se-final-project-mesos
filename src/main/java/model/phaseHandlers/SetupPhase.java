package model.phaseHandlers;

import model.GameModel;
import model.board.TurnOrderSlot;
import model.enums.GamePhase;
import model.enums.TotemLocation;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the setup phase of the game.
 * 
 * This phase is responsible for initializing the board, randomizing the turn order of players,
 * and distributing starting resources. Once setup is complete, the game transitions to the
 * {@link PlacementPhase}.
 * 
 * @see GamePhaseHandler
 * @see PlacementPhase
 * @see GameModel
 */
public class SetupPhase implements GamePhaseHandler {

    private final GameModel model;

    /**
     * Constructs a SetupPhase handler.
     *
     * @param model the game model containing game state and player information
     */
    public SetupPhase(GameModel model) {
        this.model = model;
    }

    /**
     * Initializes the setup phase.
     * 
     * This method is responsible for setting up the board, randomizing turn order, and distributing
     * starting resources to players. Once all setup tasks are complete, it transitions the game to the 
     * {@link PlacementPhase}.
     */
    @Override
    public void onEnter() {
        model.getRowsManager().setup(model.getPlayerCount());
        randomizeTurnOrder(model.getPlayers());
        distributeStartingResources();

        model.setPhase(new PlacementPhase(model));
    }

    /**
     * Randomizes the turn order by shuffling the list of players and placing their totems on the
     * TurnOrderTile in the new order. Each player is also set as being located on the TurnOrderTile.
     *
     * @param players the list of players whose turn order is to be randomized
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
     * Distributes starting resources to players based on their position in the turn order.
     * 
     * Each player receives a different amount of food based on their position:
     * Position 0 receives 2 food, positions 1-2 receive 3 food, and positions 3-4 receive 4 food.
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

    /**
     * Returns the game phase associated with this handler.
     *
     * @return the {@link GamePhase#SETUP SETUP} phase
     */
    @Override
    public GamePhase getPhase() {
        return GamePhase.SETUP;
    }

    /**
     * Returns the current player during this phase.
     * 
     * In the setup phase, there is no specific current player as setup operations
     * affect all players equally.
     *
     * @return null as no single player is active during setup
     */
    @Override
    public Player getCurrentPlayer() {
        return null;
    }
}
