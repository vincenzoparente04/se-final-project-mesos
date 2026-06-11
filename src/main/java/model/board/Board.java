package model.board;

import model.factories.BoardFactory;
import model.player.Player;

import java.util.List;

/**
 * Represents the game board managing both the offer track and turn order tile.
 * 
 * The board is responsible for coordinating totem placement and movement between
 * the offer track (where players perform actions) and the turn order tile (starting position).
 * It delegates specific operations to its constituent components while providing a unified
 * interface for board-level game logic.
 * 
 * @see OfferTrack
 * @see TurnOrderTile
 * @see OfferTile
 */
public class Board {
    private OfferTrack offerTrack;
    private TurnOrderTile turnOrderTile;

    /**
     * Constructs a new Board with components initialized for the given player count.
     * 
     * The board factory creates the appropriate offer tiles and turn order slots
     * based on the number of players.
     *
     * @param playerCount the number of players in the game
     */
    public Board(int playerCount) {
        BoardFactory.BoardComponents components = BoardFactory.createComponents(playerCount);
        this.offerTrack = new OfferTrack(components.offerTiles());
        this.turnOrderTile = new TurnOrderTile(components.turnOrderSlots(), components.turnOrderTileImage());
    }

    /**
     * Places a player's totem on a specific offer tile.
     * 
     * Removes the player's totem from its current turn order slot and places it on the
     * specified offer tile. This delegates to the offer track which then delegates to
     * the specific offer tile for handling.
     *
     * @param player the player whose totem is being placed
     * @param offerTile the target offer tile where the totem will be placed
     * @see OfferTrack#placeTotem(Player, OfferTile)
     * @see OfferTile
     */
    public void placeTotem(Player player, OfferTile offerTile) {
        turnOrderTile.freeSlot(player);
        offerTrack.placeTotem(player, offerTile);
    }
    
    /**
     * Finds an offer tile by its letter identifier.
     * 
     * Retrieves a specific offer tile from the track using its letter designation,
     * typically used for client-controller communication.
     *
     * @param letter the letter identifier of the tile to find
     * @return the {@link OfferTile} corresponding to the given letter
     * @see OfferTrack#getTileByLetter(char)
     */
    public OfferTile findTileByLetter(char letter) {
        return offerTrack.getTileByLetter(letter);
    }

    /**
     * Returns the offer track component of this board.
     *
     * @return the {@link OfferTrack} containing all offer tiles
     */
    public OfferTrack getOfferTrack() {
        return offerTrack;
    }

    /**
     * Returns the turn order tile component of this board.
     *
     * @return the {@link TurnOrderTile} managing player turn order and starting positions
     */
    public TurnOrderTile getTurnOrderTile() {
        return turnOrderTile;
    }

    /**
     * Returns the next player scheduled to act on the offer track.
     *
     * @return the next {@link Player} in the action queue, or null if the offer track is empty
     */
    public Player getNextPlayerOnOfferTrack() {
        return offerTrack != null ? offerTrack.getNextPlayer() : null;
    }

    /**
     * Removes a player's totem from the offer track and returns it to the turn order tile,
     * applying any food or prestige effects from the landing slot.
     * 
     * This is an atomic operation that concludes a player's action turn. The totem is placed
     * on the first available free slot and triggers that slot's effects.
     *
     * @param player the player whose totem is being returned
     * @see TurnOrderTile#returnTotemAndResolveEffects(Player)
     * @see OfferTrack#removeTotem(Player)
     */
    public void returnTotemToTurnOrder(Player player) {
        offerTrack.removeTotem(player);
        turnOrderTile.returnTotemAndResolveEffects(player);
    }

    /**
     * Removes a player's totem from the offer track and returns it to the turn order tile,
     * applying any food or prestige effects from the landing slot.
     * 
     * This variant is used when a player disconnects, applying the same atomic operation
     * but potentially with different handling for disconnection scenarios. The totem is
     * placed on the first available free slot and triggers that slot's effects.
     *
     * @param player the player whose totem is being returned
     * @see TurnOrderTile#disconnectedReturnTotemAndResolveEffects(Player)
     * @see OfferTrack#removeTotem(Player)
     */
    public void disconnectedReturnTotemToTurnOrder(Player player) {
        offerTrack.removeTotem(player);
        turnOrderTile.disconnectedReturnTotemAndResolveEffects(player);
    }

    /**
     * Returns the current turn order of players based on their positions on the turn order tile.
     *
     * @return a list of {@link Player}s in turn order
     */
    public List<Player> getTurnOrder() {
        return turnOrderTile.getTurnOrder();
    }

    /**
     * Returns all offer tiles on the board.
     *
     * @return a list of all {@link OfferTile}s
     */
    public List<OfferTile> getOfferTiles() {
        return offerTrack.getTiles();
    }

    /**
     * Returns all turn order slots on the turn order tile.
     *
     * @return a list of all {@link TurnOrderSlot}s
     */
    public List<TurnOrderSlot> getTurnOrderSlots() {
        return turnOrderTile.getSlots();
    }
}
