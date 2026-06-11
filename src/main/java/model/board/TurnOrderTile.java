package model.board;

import model.enums.TotemLocation;
import model.player.Player;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Manages the turn order tile component of the board.
 * 
 * The turn order tile contains a sequence of slots where players' totems are placed at the start and
 * after completing their actions. Each slot may provide food bonuses or penalties when the totem lands on it.
 * The tile is responsible for coordinating totem placement, applying slot effects, and tracking the turn order.
 * 
 * @see TurnOrderSlot
 * @see Player
 * @see TotemLocation
 */
public class TurnOrderTile {
    private List<TurnOrderSlot> slots;
    private String imagePath;

    /**
     * Constructs a TurnOrderTile with a list of slots and an image path.
     *
     * @param slots the ordered list of {@link TurnOrderSlot}s making up the turn order
     * @param image the path or identifier for the tile's display image
     */
    public TurnOrderTile(List<TurnOrderSlot> slots, String image) {
        this.slots = slots;
        this.imagePath = image;
    }

    /**
     * Clears the slot currently occupied by the given player, freeing it for future use.
     * 
     * This method is called when a player moves their totem from the turn order tile to the offer track.
     *
     * @param player the player whose slot is to be cleared
     */
    public void freeSlot(Player player) {
        slots.stream()
                .filter(s -> player.equals(s.getOccupant()))
                .findFirst()
                .ifPresent(TurnOrderSlot::removeTotem);
    }

    /**
     * Returns a player's totem to the turn order tile on the first available free slot and resolves the slot's effects.
     * 
     * Places the player on the first free slot in order, updates their location, and applies the slot's effect
     * (which may include food bonuses or penalties). This is called at the end of a player's action turn.
     *
     * @param player the player whose totem is being returned
     * @see TurnOrderSlot#applyEffect()
     * @throws NoSuchElementException if no free slot is available
     */
    public void returnTotemAndResolveEffects(Player player){
        TurnOrderSlot slot = getFirstFreeSlot();
        slot.placeTotem(player);
        player.setLocation(TotemLocation.TURN_ORDER_TILE);
        slot.applyEffect();
    }

    /**
     * Returns a player's totem to the turn order tile on the last available free slot and resolves the slot's effects.
     * 
     * This variant is used when a player disconnects. Places the player on the last free slot instead of the first,
     * updates their location, and applies the slot's effect. This may result in different game consequences than
     * the standard return flow.
     *
     * @param player the player whose totem is being returned
     * @see TurnOrderSlot#applyEffect()
     * @throws NoSuchElementException if no free slot is available
     */
    public void disconnectedReturnTotemAndResolveEffects(Player player){
        TurnOrderSlot slot = getLastFreeSlot();
        slot.placeTotem(player);
        player.setLocation(TotemLocation.TURN_ORDER_TILE);
        slot.applyEffect();
    }

    /**
     * Finds the first free slot on the turn order tile.
     * 
     * Searches the list of slots in order and returns the first unoccupied slot.
     *
     * @return the first free {@link TurnOrderSlot}
     * @throws NoSuchElementException if no free slot is found
     */
    private TurnOrderSlot getFirstFreeSlot() {
        return slots.stream()
                .filter(TurnOrderSlot::isFree)
                .findFirst()
                .orElseThrow();
    }

    /**
     * Finds the last free slot on the turn order tile.
     * 
     * Searches the list of slots and returns the final unoccupied slot. Used when a player
     * disconnects to apply a different turn order outcome.
     *
     * @return the last free {@link TurnOrderSlot}
     * @throws NoSuchElementException if no free slot is found
     */
    private TurnOrderSlot getLastFreeSlot() {
        return slots.stream()
                .filter(TurnOrderSlot::isFree)
                .reduce((first, second) -> second) //get last free slot
                .orElseThrow();
    }

    /**
     * Returns the list of players in turn order based on their slot positions.
     * 
     * Constructs a list containing all players currently occupying slots, in the order
     * that their slots appear on the turn order tile.
     *
     * @return a list of {@link Player}s in turn order
     */
    public List<Player> getTurnOrder(){
        return slots.stream().filter(TurnOrderSlot::isOccupied).map(TurnOrderSlot::getOccupant).toList();
    }

    /**
     * Returns all turn order slots on this tile.
     *
     * @return a list of all {@link TurnOrderSlot}s
     */
    public List<TurnOrderSlot> getSlots() {
        return slots;
    }
}