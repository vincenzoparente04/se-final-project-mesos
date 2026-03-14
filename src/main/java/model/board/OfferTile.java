package model.board;

import model.player.Totem;

// Box for the totem
public class OfferTile {
    private final char letter;
    private final OfferTileAction action;  // number of cards to pick and form which row
    private final int foodBonus;           // 0 if no bonus
    private Totem occupant;                // null if free

    public boolean isOccupied(){
        return occupant != null;
    }

    public void placeTotem(Totem totem){
        this.occupant = totem;
    }


    public Totem getOccupant(){
        return occupant;
    }





    public void removeTotem()
    public OfferTileAction getAction()
    public int getFoodBonus()
    public char getLetter()
}