package model.board;

import model.player.Player;
import model.player.Totem;

import java.util.List;

// First tile of the track that determines the turn order for the next round and gives food bonuses to players based on their position
public class TurnOrderTile {
    private final int playerCount; // number of players in the game
    private List<Totem> slots;


    public TurnOrderTile(int playerCount)

    public void setup(int playerCount)
    // configura il numero di slot corretti

    public void placeTotemAtSlot(Totem totem, int slotIndex){
    }

    // returns the turn order for the next round
    // it is simply the order of the slots from top to bottom
    public List<Player> getTurnOrder(){}







    // place back the totems from the first available slot from the top to the bottom
    // called by the Controller after a player resolves their action
    public void placeBackTotem(Totem totem)



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