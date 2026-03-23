package model.board;

import model.enums.TotemLocation;
import model.player.Player;

import java.util.List;

public class TurnOrderTile {
    private final int playerCount;
    private final List<TurnOrderSlot> slots;
    // gli slot vengono costruiti nel costruttori in base al numero di giocatori

    /**
     * @implNote Places the player's totem on the first free slot of the TurnOrderTile and applies the effect of that slot.
     * It first finds the first free slot using the getFirstFreeSlot() method, then places the player's totem on that slot
     * and sets the player's location to indicate that their totem is on the TurnOrderTile. Finally, it applies the effect
     * of the slot, which may involve granting food bonuses or other benefits based on the specific implementation of the TurnOrderSlot class.
     * @param player
     */
    public void returnTotemAndResolveEffects(Player player){
        TurnOrderSlot slot = getFirstFreeSlot();
        slot.placeTotem(player);
        player.setLocation(TotemLocation.TURN_ORDER_TILE);
        slot.applyEffect();
    }

    /**
     * @implNote Finds the first free slot on the TurnOrderTile. It filters the list of slots to find the
     * first one that is free (i.e., has no occupant) and returns it. If no free slot is found, it throws an exception.
     * @return
     */
    private TurnOrderSlot getFirstFreeSlot() {
        return slots.stream()
                .filter(TurnOrderSlot::isFree)
                .findFirst()
                .orElseThrow();
    }

    // SERVE???
    //public void placeTotemAtSlot(Player player, int index){}

    /**
     * @implNote Returns the list of players in the order determined by the occupied slots.
     * It filters the slots to include only those that are occupied, then maps each occupied slot to its occupant (the player) and collects them into a list.
     * @return
     */
    public List<Player> getTurnOrder(){
        return slots.stream().filter(TurnOrderSlot::isOccupied).map(TurnOrderSlot::getOccupant).toList();
    }

    public List<TurnOrderSlot> getSlots() {
        return slots;
    }

    public void setup(int playerCount) {
        // costruisce gli slot con i bonus corretti per playerCount
        // es. per 3 giocatori:
        // slot 0 → foodBonus=3, isLast=false
        // slot 1 → foodBonus=1, isLast=false
        // slot 2 → foodBonus=0, isLast=true
    }
}