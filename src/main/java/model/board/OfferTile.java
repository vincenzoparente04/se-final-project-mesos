package model.board;

import model.board.OfferTileAction.OfferTileAction;
import model.player.Player;

// Box for the totem
public class OfferTile {
    private final char letter;
    private final OfferTileAction action;
    private final String frontImage;
    private final String backImage;
    private Player occupant;                // null if free

    public OfferTile(char letter, OfferTileAction action, String frontImage, String backImage) {
        this.letter = letter;
        this.action = action;
        this.frontImage = frontImage;
        this.backImage = backImage;
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

    public char getLetter() {
        return letter;
    }
}