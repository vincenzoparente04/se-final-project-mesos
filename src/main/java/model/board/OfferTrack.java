package model.board;

import model.enums.TotemLocation;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

public class OfferTrack {
    private List<OfferTile> tiles = new ArrayList<>();  // listed from A to G

    public void setup(List<OfferTile> tiles) {
        this.tiles = tiles; // lista già filtrata e ordinata dalla factory
    }

    /**
     * @implNote Checks if the tile is occupied, if so throws an exception. It places the totem on the tile and set its location.
     */
    public void placeTotem(Player player, OfferTile offerTile) {
        offerTile.placeTotem(player);
        player.setLocation(TotemLocation.OFFER_TRACK);
    }

    /**
     * @implNote Find the correct tile based on the char passed by the view to the controller
     * @return
     */
    public OfferTile getTileByLetter(char letter) {
        return tiles.stream()
                .filter(tile -> tile.getLetter() == letter)
                .findFirst()
                .orElse(null);
    }

    public List<OfferTile> getTiles() {
        return tiles;
    }

    public Player getNextPlayer() {
        return tiles.stream().filter(OfferTile::isOccupied).findFirst().map(OfferTile::getOccupant).orElse(null);
    }

    //secondo me inutile
    //public List<OfferTile> getOccupiedTilesInOrder() {
    //    return tiles;
    //}// listed left to right, used to determine who resolves actions and in which order

    public OfferTile getOccupiedTileByPlayer(Player player) {
        return tiles.stream()
                .filter(tile -> player.equals(tile.getOccupant()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Removes the player's totem from whichever offer tile they occupy.
     * Called at the end of each action turn before moving to the turn order tile.
     */
    public void removeTotem(Player player) {
        OfferTile tile = getOccupiedTileByPlayer(player);
        if (tile != null) {
            tile.removeTotem();
        }
    }
}