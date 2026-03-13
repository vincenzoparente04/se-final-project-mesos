package model.board;

import model.enums.TotemLocation;
import model.player.Totem;

import java.util.List;

public class OfferTrack {
    private final List<OfferTile> tiles;  // listed from A to G

    public void setup(int playerCount)
    // configura le caselle corrette per il numero di giocatori

    /**
     * @implNote Checks if the tile is occupied, if so throws an exception. It places the totem on the tile and set its location.
     * @param totem
     * @param offerTile
     * @throws Exception
     */
    public void placeTotem(Totem totem, OfferTile offerTile) throws Exception{
        if(offerTile.isOccupied()){
            throw new Exception("Cannot place the totem in an occupied tile");
        }
        offerTile.placeTotem(totem);
        totem.setLocation(TotemLocation.OFFER_TRACK);
    }


    public List<OfferTile> getOccupiedTilesInOrder() // listed left to right, used to determine who resolves actions and in which order









    public OfferTile getTile(char letter)
    public List<OfferTile> getAvailableTiles()     // with no Totem on top
}