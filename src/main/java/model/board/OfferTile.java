package model.board;

import model.board.OfferTileAction.OfferTileAction;
import model.player.Player;

// Box for the totem
public class OfferTile {
    private final char letter;
    private final OfferTileAction action;  // number of cards to pick and form which row
    private final int foodBonus;           // 0 if no bonus
    private Player occupant;                // null if free

    public OfferTile(char letter, OfferTileAction action, int foodBonus) {
        this.letter = letter;
        this.action = action;
        this.foodBonus = foodBonus;
    }

    public boolean isOccupied(){
        return occupant != null;
    }

    public void placeTotem(Player player){
        this.occupant = player;
    }

    public Player getOccupant(){
        return occupant;
    }

    // TODO
    public void removeTotem()
    public OfferTileAction getAction()
    public int getFoodBonus()
    public char getLetter()
}