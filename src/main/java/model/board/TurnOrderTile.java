package model.board;

import model.enums.TotemLocation;
import model.player.Player;

import java.util.List;

// First tile of the track that determines the turn order for the next round and gives food bonuses to players based on their position
public class TurnOrderTile {
    private final int playerCount
    private final List<TurnOrderSlot> slots;
    // gli slot vengono costruiti nel costruttori in base al numero di giocatori

    public void returnTotem(Player player){
        TurnOrderSlot slot = getFirstFreeSlot();
        slot.placeTotem(player;
        player.setLocation(TotemLocation.TURN_ORDER_TILE);
        slot.applyEffect();
    }

    private TurnOrderSlot getFirstFreeSlot() {
        // scorre la lista degli slot già creati durante il setup
        // restituisce il riferimento al primo che ha occupant == null
        return slots.stream()
                .filter(TurnOrderSlot::isFree)
                .findFirst()
                .orElseThrow();
    }

    public void placeTotemAtSlot(Player player, int index){}

    public List<Player> getTurnOrder()
    // scorre gli slot in ordine
    // per ogni slot occupato restituisce slot.getOccupant().getOwner()

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