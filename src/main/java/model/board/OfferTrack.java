package model.board;

import model.enums.TotemLocation;
import model.player.Player;

import java.util.List;

/**
 * Manages the offer track component of the board.
 * 
 * The offer track is a sequence of tiles (labeled A through G) where players place their totems
 * to perform actions. It tracks which tiles are occupied and provides queries for retrieving
 * tiles and coordinating player actions.
 * 
 * @see OfferTile
 * @see Player
 * @see TotemLocation
 */
public class OfferTrack {
    private List<OfferTile> tiles;  // listed from A to G

    /**
     * Constructs an OfferTrack with a given list of tiles.
     * 
     * The tiles are expected to be pre-filtered and ordered by the factory.
     *
     * @param tiles the ordered list of {@link OfferTile}s from A to G
     */
    public OfferTrack(List<OfferTile> tiles) {
        this.tiles = tiles; // lista già filtrata e ordinata dalla factory
    }

    /**
     * Places a player's totem on a specific offer tile and updates the player's location.
     * 
     * Sets the player's totem on the given tile and marks the player's location as
     * being on the offer track.
     *
     * @param player the player whose totem is being placed
     * @param offerTile the target {@link OfferTile} where the totem will be placed
     * @see OfferTile#placeTotem(Player)
     * @see Player#setLocation(TotemLocation)
     */
    public void placeTotem(Player player, OfferTile offerTile) {
        offerTile.placeTotem(player);
        player.setLocation(TotemLocation.OFFER_TRACK);
    }

    /**
     * Finds an offer tile by its letter identifier.
     * 
     * Performs a linear search through the tiles to locate the one matching the given letter.
     * Used to map client input (a letter) to the corresponding tile.
     *
     * @param letter the letter identifier of the tile to find (typically A-G)
     * @return the {@link OfferTile} with the given letter, or null if not found
     */
    public OfferTile getTileByLetter(char letter) {
        return tiles.stream()
                .filter(tile -> tile.getLetter() == letter)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns all offer tiles on this track.
     *
     * @return a list of all {@link OfferTile}s in order (A to G)
     */
    public List<OfferTile> getTiles() {
        return tiles;
    }

    /**
     * Returns the first player encountered on any occupied tile in track order.
     * 
     * This typically represents the next player whose action will be resolved.
     *
     * @return the {@link Player} occupying the first occupied tile, or null if no tiles are occupied
     */
    public Player getNextPlayer() {
        return tiles.stream().filter(OfferTile::isOccupied).findFirst().map(OfferTile::getOccupant).orElse(null);
    }

    /**
     * Finds the offer tile occupied by a specific player.
     *
     * @param player the player to search for
     * @return the {@link OfferTile} occupied by the player, or null if the player is not on the offer track
     */
    public OfferTile getOccupiedTileByPlayer(Player player) {
        return tiles.stream()
                .filter(tile -> player.equals(tile.getOccupant()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Removes a player's totem from whichever offer tile they currently occupy.
     * 
     * This operation is called at the end of a player's action turn before the totem
     * is returned to the turn order tile. If the player is not on the offer track,
     * this method performs no operation.
     *
     * @param player the player whose totem is to be removed
     */
    public void removeTotem(Player player) {
        OfferTile tile = getOccupiedTileByPlayer(player);
        if (tile != null) {
            tile.removeTotem();
        }
    }
}