package model.board;

import model.player.Totem;

public class TurnOrderSlot {
    private final int foodBonus;   // fisso — 0 se nessun bonus
    private final boolean isLast;  // fisso — true solo per l'ultimo slot
    private Totem occupant;        // variabile — null se lo slot è libero

    public boolean isOccupied()     { return occupant != null; }
    public boolean isFree()         { return occupant == null; }
    public int getFoodBonus()       { return foodBonus; }
    public boolean isLast()         { return isLast; }
    public Totem getOccupant()      { return occupant; }
    public void placeTotem(Totem totem) { this.occupant = totem; }
}