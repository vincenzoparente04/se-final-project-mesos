package model.board;



// Box for the totem
public class OfferTile {
    private final char letter;
    private final OfferTileAction action;  // number of cards to pick and form which row
    private final int foodBonus;           // 0 if no bonus
    private Totem occupant;                // null if free

    public boolean isOccupied()
    public void placeTotem(Totem totem)
    public void removeTotem()
    public Totem getOccupant()
    public OfferTileAction getAction()
    public int getFoodBonus()
    public char getLetter()
}