package model.board;

// First tile of the track that determines the turn order for the next round and gives food bonuses to players based on their position
public class TurnOrderTile {
    private final int playerCount; // number of players in the game
    private final List<Totem> slots;
    // slots.get(0) = first place (who goes first in the next round)
    // slots.get(playerCount-1) = last place (food penalty)


    public TurnOrderTile(int playerCount)

    // place back the totems from the first available slot from the top to the bottom
    // called by the Controller after a player resolves their action
    public void placeBackTotem(Totem totem)

    // returns the turn order for the next round
    // it is simply the order of the slots from top to bottom
    public List<Totem> getTurnOrder()

    // get the last placed totem — used by the Controller to apply the penalty
    public Totem getLastPlacedTotem()

    // bonus food slots: some slots give food to whoever occupies them
    // REFACTOR: da rivedere, probabilmente usato da actionManager in roundManager
    public int getFoodBonusForSlot(int slotIndex)

    // removes all totems at the beginning of the placement round
    // da rivedere
    public void clearAllTotems()

    public boolean isFull()

    // used by placeBackTotem()
    public int getFirstAvailableSlotIndex()
}