package model.player;

import model.enums.TotemColor;
import model.enums.TotemLocation;

/**
 * Represents a player participating in the game, encapsulating their persistent
 * identity, resources, and state.
 *
 * <p>Each player owns:
 * <ul>
 *   <li>A {@code name} and {@link Tribe}, established at construction time.</li>
 *   <li>Mutable resources — {@code food} and {@code prestigePoints} — manipulated
 *       through dedicated add/remove operations that enforce the game's economy rules.</li>
 *   <li>A {@link TotemLocation} tracking the current position of the player's totem
 *       on the board, and a {@link TotemColor} assigned during the
 *       {@link model.phaseHandlers.ColorChoosingPhase}.</li>
 *   <li>A set of boolean flags ({@code shamanicImmunity},
 *       {@code shamanicBonusStars}, {@code shamanicDoublePrestige},
 *       {@code extraFoodOnTotemReturn}, {@code extraDraw}) that grant
 *       persistent or round-scoped bonuses awarded by specific buildings.</li>
 *   <li>A connection flag ({@code online}) used by the phase handlers to
 *       skip disconnected players while preserving the turn order.</li>
 * </ul>
 */
public class Player {
    private final String name;
    private final Tribe tribe;
    private int food;
    private int prestigePoints;
    private TotemLocation totemLocation;
    private TotemColor color;

    private boolean shamanicImmunity = false;
    private boolean shamanicBonusStars = false;
    private boolean shamanicDoublePrestige = false;
    private boolean extraFoodOnTotemReturn = false;
    private boolean extraDraw = false;
    private boolean online;

    /**
     * Constructs a new player with the specified name.
     * 
     * Initializes the player with default values: 0 food, 0 prestige points,
     * the tribe, location on turn order tile, no color assigned, and online status.
     *
     * @param name the unique name of the player
     */
    public Player(String name) {
        this.name = name;
        this.prestigePoints = 0;
        this.food = 0;
        this.tribe = new Tribe();
        this.totemLocation = TotemLocation.TURN_ORDER_TILE; // default location at the start of the game
        this.color = null;
        this.online = true;
    }

    /**
     * Returns the current amount of food possessed by this player.
     *
     * @return the food count
     */
    public int getFood(){ 
        return food; 
    }

    /**
     * Adds the specified amount of food to the player's stock.
     *
     * @param amount the amount of food to add
     */
    public void addFood(int amount){ 
        food += amount; 
    }

    /**
     * Returns the current prestige points of this player.
     *
     * @return the prestige point count
     */
    public int getPrestigePoints() { 
        return prestigePoints; 
    }

    /**
     * Adds the specified amount of prestige points to the player.
     *
     * @param amount the amount of prestige points to add
     */
    public void addPrestigePoints(int amount) { 
        prestigePoints += amount; 
    }

    /**
     * Removes the specified amount of prestige points from the player.
     *
     * @param amount the amount of prestige points to remove
     */
    public void removePrestigePoints(int amount) { 
        prestigePoints -= amount; 
    }

    /**
     * Removes the given amount of food from the player, clamping the result at zero
     * so that the food stock can never become negative.
     *
     * <p>This variant is used when the rules prescribe no further penalty for
     * insufficient food: any unpaid portion of the cost is simply discarded.
     *
     * @param amount the quantity of food to remove;
     */
    public void removeFood(int amount) {
        food = Math.max(0, food - amount);
    }

    /**
     * Removes the given amount of food from the player and, if the stock is
     * insufficient, converts the uncovered deficit into a loss of prestige points
     * weighted by the provided multiplier.
     *
     * <p>Unlike {@link #removeFood(int)}, which silently absorbs any shortfall,
     * this method enforces a penalty: each missing unit of food costs the player
     * {@code prestigeMultiplier} prestige points. It is intended for game effects
     * that explicitly mandate a prestige cost when food cannot fully cover the
     * required payment.
     *
     * @param amount             the quantity of food the player is required to pay.
     * @param prestigeMultiplier the prestige points subtracted for each unit of
     *                           food that the player could not cover.
     */
    public void removeFoodWithPrestigePenalty(int amount, int prestigeMultiplier) {
        int available = food;
        if (amount <= available) {
            food -= amount;
        } else {
            food = 0;
            removePrestigePoints((amount - available) * prestigeMultiplier);
        }
    }

    /**
     * Returns the tribe of this player.
     *
     * @return the {@link Tribe} containing all character and building cards
     */
    public Tribe getTribe(){ 
        return tribe; 
    }

    /**
     * Determines whether this player has shamanic immunity.
     *
     * @return true if the player has shamanic immunity, false otherwise
     */
    public boolean hasShamanicImmunity() {
        return shamanicImmunity;
    }

    /**
     * Determines whether this player has shamanic double prestige bonus.
     *
     * @return true if the player has shamanic double prestige, false otherwise
     */
    public boolean hasShamanicDoublePrestige() {
        return shamanicDoublePrestige;
    }

    /**
     * Determines whether this player has shamanic bonus icons.
     *
     * @return true if the player has shamanic bonus icons, false otherwise
     */
    public boolean hasShamanicBonusIcons() {
        return shamanicBonusStars;
    }

    /**
     * Determines whether this player has extra food on totem return.
     *
     * @return true if the player has the extra food on totem return building, false otherwise
     */
    public boolean hasExtraFoodOnTotemReturn() {
        return extraFoodOnTotemReturn;
    }

    /**
     * Determines whether this player has an extra draw available.
     *
     * @return true if the player has an extra draw, false otherwise
     */
    public boolean hasExtraDraw() {
        return extraDraw;
    }

    /**
     * Sets the shamanic immunity status of this player.
     *
     * @param shamanicImmunity true to grant shamanic immunity, false to remove it
     */
    public void setShamanicImmunity(boolean shamanicImmunity) {
        this.shamanicImmunity = shamanicImmunity;
    }

    /**
     * Sets the shamanic bonus icons status of this player.
     *
     * @param shamanicBonusStars true to grant shamanic bonus icons, false to remove it
     */
    public void setShamanicBonusIcons(boolean shamanicBonusStars) {
        this.shamanicBonusStars = shamanicBonusStars;
    }

    /**
     * Sets the shamanic double prestige status of this player.
     *
     * @param shamanicDoublePrestige true to grant double prestige bonus, false to remove it
     */
    public void setShamanicDoublePrestige(boolean shamanicDoublePrestige) {
        this.shamanicDoublePrestige = shamanicDoublePrestige;
    }

    /**
     * Sets the extra food on totem return status of this player.
     *
     * @param extraFoodOnTotemReturn true to grant extra food on totem return, false to remove it
     */
    public void setExtraFoodOnTotemReturn(boolean extraFoodOnTotemReturn) {
        this.extraFoodOnTotemReturn = extraFoodOnTotemReturn;
    }

    /**
     * Sets the extra draw status of this player.
     * 
     * Called when the player draws a building that grants an extra draw.
     *
     * @param extraDraw true to grant an extra draw, false to remove it
     */
    public void setExtraDraw(boolean extraDraw) {
        this.extraDraw = extraDraw;
    }

    /**
     * Sets the location of this player's totem on the board.
     *
     * @param location the {@link TotemLocation} where the totem is positioned
     */
    public void setLocation(TotemLocation location) {
        this.totemLocation = location;
    }

    /**
     * Sets the color of this player's totem.
     *
     * @param color the {@link TotemColor} assigned to this player
     */
    public void setColor(TotemColor color) {
        this.color = color;
    }

    /**
     * Determines whether this player is currently connected.
     *
     * @return true if the player is online, false if disconnected
     */
    public boolean isConnected(){
        return this.online;
    }

    /**
     * Marks this player as connected (online).
     */
    public void setConnected(){
        this.online = true;
    }

    /**
     * Marks this player as disconnected (offline).
     */
    public void setDisconnected(){
        this.online = false;
    }

    /**
     * Returns the name of this player.
     *
     * @return the player name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the totem color assigned to this player.
     *
     * @return the {@link TotemColor}, or null if not yet assigned
     */
    public TotemColor getColor() {
        return color;
    }

    /**
     * Returns the current location of this player's totem.
     *
     * @return the {@link TotemLocation} of the totem
     */
    public TotemLocation getLocation() {
        return totemLocation;
    }
}