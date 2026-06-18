package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.board.OfferTile;
import model.enums.GamePhase;
import model.enums.TotemLocation;
import model.player.Player;
import shared.command.gameCommand.PlaceTotemCommand;

import java.util.List;

/**
 * Orchestrates the sequential totem placement phase, where players select
 * their desired offer tiles for the upcoming round based on the established turn order.
 *
 * <p>This handler manages an internal stateful iteration over the players, ensuring
 * strict adherence to the round's sequence. Its lifecycle encompasses:
 * <ol>
 * <li><b>Initialization:</b> Snapshotting the current turn order and cueing the first active player.</li>
 * <li><b>Command Processing:</b> Intercepting {@link PlaceTotemCommand}s via double-dispatch,
 * rigorously validating turn authorization, tile existence, and tile availability.</li>
 * <li><b>Fault Tolerance:</b> Automatically skipping the turns of disconnected players
 * to prevent game deadlocks, ensuring their totems are handled via a fallback mechanism.</li>
 * <li><b>State Transition:</b> Automatically advancing the global game state to the
 * {@link ActionPhase} once all valid totems have been successfully placed on the board.</li>
 * </ol>
 *
 * @see GamePhaseHandler
 * @see model.board.Board
 * @see shared.command.gameCommand.PlaceTotemCommand
 * @see model.phaseHandlers.ActionPhase
 */
public class PlacementPhase implements GamePhaseHandler {

    private final GameModel model;
    private List<Player> turnOrder;
    private int currentIndex;
    private Player currentPlayer;

    public PlacementPhase(GameModel model) {
        this.model = model;
    }

    /**
     * Bootstraps the phase by loading the current turn order
     * and setting the execution pointer to the first player.
     * <p>
     * If the first player in the sequence is detected as disconnected upon entry,
     * the system immediately delegates to the turn-advancement logic to maintain flow.
     * </p>
     */
    @Override
    public void onEnter() {
        turnOrder = model.getTurnOrder();
        currentIndex = 0;
        currentPlayer = turnOrder.get(currentIndex);

        if (!currentPlayer.isConnected()) {
            advanceTurn();
            return;
        }

        model.notifyChange();
    }


    /**
     * Intercepts and processes a client's request to place their totem on a specific tile.
     * <p>
     * This method acts as a strict validator before allowing model mutation. It ensures
     * the command originates from the active player and targets a valid, unoccupied tile.
     * Upon successful validation, it delegates the physical placement to the {@link Board}
     * and advances the internal turn sequence.
     * </p>
     *
     * @param cmd the payload containing the player's identity and their targeted tile ID.
     * @throws IllegalArgumentException if the targeted tile does not exist, if the tile
     * is already occupied, or if the requesting player is not the current active player.
     * @throws Exception if an unexpected error occurs during board mutation.
     */
    @Override
    public void visit(PlaceTotemCommand cmd) throws Exception {
        Player player = model.getPlayerByName(cmd.playerName());
        char tileId = cmd.tileId();

        Board board = model.getBoard();
        OfferTile offerTile = board.findTileByLetter(tileId);
        if (offerTile == null) {
            throw new IllegalArgumentException("tileId " + tileId + " is invalid");
        }

        if (player != currentPlayer) {
            //checked also in gameController
            throw new IllegalArgumentException("It's not " + player.getName() + "'s turn to place a totem");
        }
        if (currentPlayer.getLocation() != TotemLocation.TURN_ORDER_TILE) {
            throw new IllegalArgumentException("Player " + player.getName() + " cannot place a totem because he is not on the turn order tile");
        }
        if (offerTile.isOccupied()) {
            throw new IllegalArgumentException("Tile " + offerTile.getLetter() + " is already occupied");
        }

        board.placeTotem(player, offerTile);
        advanceTurn();
    }


    /**
     * Progresses the state to the next available player in the turn order.
     * <p>
     * This method encapsulates the iteration logic, automatically skipping over
     * any disconnected players and moving their totem to first free slot from the bottom.
     * When the sequence is exhausted (all players processed),
     * it executes a cleanup routine for disconnected players (freeing their slots and
     * triggering fallback effects) before permanently transitioning the game to the {@code ActionPhase}.
     * </p>
     */
    private void advanceTurn() {
        do {
            currentIndex++;
        } while (currentIndex < turnOrder.size() && !turnOrder.get(currentIndex).isConnected());

        if (currentIndex < turnOrder.size()) {
            currentPlayer = turnOrder.get(currentIndex);
            model.notifyChange();
        } else {
            for(Player p: model.getPlayers()) {
                if (!p.isConnected()) {
                    model.getBoard().getTurnOrderTile().freeSlot(p);
                    model.getBoard().getTurnOrderTile().disconnectedReturnTotemAndResolveEffects(p);
                }
            }
            // all totems placed -> move to action phase
            model.setPhase(new ActionPhase(model));
            model.notifyChange();
        }
    }

    @Override
    public void skipCurrentPlayerTurn() {
        advanceTurn();
    }

    @Override
    public GamePhase getPhase() { return GamePhase.PLACEMENT; }

    @Override
    public Player getCurrentPlayer() { return currentPlayer; }
}
