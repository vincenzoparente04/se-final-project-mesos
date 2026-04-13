package model.board;

import model.player.Player;

public class TurnOrderSlot {
    private final int foodBonus;
    private final boolean isLast;
    private Player occupant;

    public TurnOrderSlot(int foodBonus, boolean isLast) {
        this.foodBonus = foodBonus;
        this.isLast = isLast;
        this.occupant = null;
    }

    public void applyEffect(){
        if (isLast) {
            this.occupant.removeFoodWithPrestigePenalty(1, 2);
        }

        if (foodBonus > 0) {
            this.occupant.addFood(getFoodBonus());
        }
    }

    public boolean isOccupied()     { return occupant != null; }
    public boolean isFree()         { return occupant == null; }
    public int getFoodBonus()       { return foodBonus; }
    public boolean isLast()         { return isLast; }
    public Player getOccupant()      { return occupant; }
    public void placeTotem(Player player) { this.occupant = player; }
    public void removeTotem()            { this.occupant = null; }
}