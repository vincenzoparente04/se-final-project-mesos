package model.board;

import model.board.OfferTileAction.OfferTileAction;
import model.player.Player;

/**
 * Represents a single offer tile on the board where a player's totem can be placed.
 * 
 * Each offer tile has a unique letter identifier, an associated action, and images
 * for display. At any time, the tile can be either free or occupied by a single player's totem.
 * 
 * @see OfferTileAction
 * @see Player
 */
public class OfferTile {
    private final char letter;
    private final OfferTileAction action;
    private final String frontImage;
    private final String backImage;
    private Player occupant;                // null if free

    /**
     * Constructs an OfferTile with a letter identifier, action, and display images.
     *
     * @param letter the unique letter identifier for this tile
     * @param action the {@link OfferTileAction} to execute when a player lands on this tile
     * @param frontImage the path or identifier for the front display image
     * @param backImage the path or identifier for the back display image
     */
    public OfferTile(char letter, OfferTileAction action, String frontImage, String backImage) {
        this.letter = letter;
        this.action = action;
        this.frontImage = frontImage;
        this.backImage = backImage;
    }

    /**
     * Determines whether this tile is currently occupied by a player's totem.
     *
     * @return true if a player's totem is on this tile, false if the tile is free
     */
    public boolean isOccupied(){
        return occupant != null;
    }

    /**
     * Places a player's totem on this tile.
     * 
     * The tile must be free before calling this method.
     *
     * @param player the player whose totem is being placed
     */
    public void placeTotem(Player player){
        this.occupant = player;
    }

    /**
     * Returns the player whose totem currently occupies this tile.
     *
     * @return the occupying {@link Player}, or null if the tile is free
     */
    public Player getOccupant(){
        return occupant;
    }

    /**
     * Removes the player's totem from this tile, freeing it.
     */
    public void removeTotem() {
        this.occupant = null;
    }

    /**
     * Returns the action associated with this tile.
     *
     * @return the {@link OfferTileAction} to execute when a player lands on this tile
     */
    public OfferTileAction getAction() {
        return action;
    }

    /**
     * Returns the letter identifier of this tile.
     *
     * @return the char letter identifier
     */
    public char getLetter() {
        return letter;
    }
}