package model.board;

import java.util.List;

public class OfferTrack {
    private final List<OfferTile> tiles;  // listed from A to G

    public OfferTile getTile(char letter)
    public List<OfferTile> getAvailableTiles()     // with no Totem on top
    public List<OfferTile> getOccupiedTilesInOrder() // listed left to right, used to determine who resolves actions and in which order
}