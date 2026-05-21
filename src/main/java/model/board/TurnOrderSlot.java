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

    /**
     * @implNote last slot -> remove food or prestige
     * for the others slot, if they have a food bonus > 0 -> add food to the player, checking if the player has the extra food on totem return building.
     */
    public void applyEffect(){
        if (isLast) {
            this.occupant.removeFoodWithPrestigePenalty(1, 2);
        }

        if (foodBonus > 0) {
            this.occupant.addFood(getFoodBonus());
            // apply the effect of the extra food on totem return building
            if (this.occupant.hasExtraFoodOnTotemReturn()) {
                this.occupant.addFood(1);
            }
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