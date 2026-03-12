package model.board;

import model.enums.TotemLocation;
import model.player.Totem;

import java.util.List;

public class OfferTrack {
    private final List<OfferTile> tiles;  // listed from A to G

    public void setup(int playerCount)
    // configura le caselle corrette per il numero di giocatori

    public void placeTotem(Totem totem, OfferTile offerTile){
        offerTile.placeTotem(totem);
        totem.setLocation(TotemLocation.OFFER_TRACK);
    }
    public List<OfferTile> getOccupiedTilesInOrder() // listed left to right, used to determine who resolves actions and in which order









    public OfferTile getTile(char letter)
    public List<OfferTile> getAvailableTiles()     // with no Totem on top
}