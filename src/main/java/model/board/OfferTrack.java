package model.board;

import model.enums.TotemLocation;
import model.player.Player;

import java.util.List;

public class OfferTrack {
    private final List<OfferTile> tiles;  // listed from A to G

    public void setup(int playerCount){
    }
    // configura le caselle corrette per il numero di giocatori

    /**
     * @implNote Checks if the tile is occupied, if so throws an exception. It places the totem on the tile and set its location.
     * @param player
     * @param offerTile
     * @throws Exception
     */
    public void placeTotem(Player player, OfferTile offerTile) throws Exception{
        offerTile.placeTotem(player);
        player.setLocation(TotemLocation.OFFER_TRACK);
    }

    public Player getNextPlayer() {
        return tiles.stream().filter(OfferTile::isOccupied).findFirst().map(OfferTile::getOccupant).orElse(null);
    }

    //secondo me inutile
    //public List<OfferTile> getOccupiedTilesInOrder() {
    //    return tiles;
    //}// listed left to right, used to determine who resolves actions and in which order

    public OfferTile getOccupiedTileByPlayer(Player player)
    // scorre le tile e restituisce quella il cui occupant
    // è il totem del player





    public OfferTile getTile(char letter) {}
    public List<OfferTile> getAvailableTiles()     // with no Totem on top
}