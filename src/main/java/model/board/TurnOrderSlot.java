package model.board;

import model.player.Player;

/**
 * Represents a slot in the turn order tile where a player's totem can be placed.
 * 
 * Each slot has an associated food bonus and may be designated as the last slot,
 * which applies penalties instead of bonuses. When a player's totem returns to a slot,
 * the slot's effect is resolved (food bonus or penalty).
 * 
 * @see Player
 * @see TurnOrderTile
 */
public class TurnOrderSlot {
    private final int foodBonus;
    private final boolean isLast;
    private Player occupant;

    /**
     * Constructs a TurnOrderSlot with a specified food bonus and last slot designation.
     *
     * @param foodBonus the amount of food granted when this slot's effect is applied (0 or positive)
     * @param isLast true if this is the last slot with a penalty effect, false otherwise
     */
    public TurnOrderSlot(int foodBonus, boolean isLast) {
        this.foodBonus = foodBonus;
        this.isLast = isLast;
        this.occupant = null;
    }

    /**
     * Applies the slot's effect to the occupant player.
     * 
     * If this is the last slot, applies a food/prestige penalty to the player.
     * Otherwise, if the slot has a positive food bonus, grants the bonus to the player.
     * Additionally, if the player has the "extra food on totem return" building, grants
     * an extra food point when the bonus is applied.
     * 
     * @throws NullPointerException if the slot is not occupied when this method is called
     * @see Player#removeFoodWithPrestigePenalty(int, int)
     * @see Player#addFood(int)
     * @see Player#hasExtraFoodOnTotemReturn()
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