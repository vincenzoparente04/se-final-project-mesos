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

    public void removeTotem() {
        this.occupant = null;
    }

    public OfferTileAction getAction() {
        return action;
    }

    public int getFoodBonus() {
        return foodBonus;
    }

    public char getLetter() {
        return letter;
    }
}