package model.board;

import model.player.Player;
import model.player.Totem;

import java.util.List;

// First tile of the track that determines the turn order for the next round and gives food bonuses to players based on their position
public class TurnOrderTile {
    private final int playerCount
    private final List<TurnOrderSlot> slots;
    // gli slot vengono costruiti nel costruttori in base al numero di giocatori

    public TurnOrderSlot returnTotem(Player player)
    // trova il primo slot libero dall'alto (isFree())
    // chiama slot.placeTotem(player.getTotem())
    // restituisce lo slot in cui è atterrato

    public List<Player> getTurnOrder()
    // scorre gli slot in ordine
    // per ogni slot occupato restituisce slot.getOccupant().getOwner()

    public void setup(int playerCount) {
        // costruisce gli slot con i bonus corretti per playerCount
        // es. per 3 giocatori:
        // slot 0 → foodBonus=3, isLast=false
        // slot 1 → foodBonus=1, isLast=false
        // slot 2 → foodBonus=0, isLast=true
    }
}