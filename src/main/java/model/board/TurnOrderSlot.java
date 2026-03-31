package model.board;

import model.player.Player;

public class TurnOrderSlot {
    private final int foodBonus;   // fisso — 0 se nessun bonus
    private final boolean isLast;  // fisso — true solo per l'ultimo slot
    private Player occupant;        // variabile — null se lo slot è libero

    public TurnOrderSlot(int foodBonus, boolean isLast) {
        this.foodBonus = foodBonus;
        this.isLast = isLast;
        this.occupant = null;
    }

    public void applyEffect(){
        if (isLast) {
            this.occupant.removeFood(1, 2);
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
}