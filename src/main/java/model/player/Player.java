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

    public int getFood(){ return food; }
    public void addFood(int amount){ food += amount; }
    public int getPrestigePoints() { return prestigePoints; }
    public void addPrestigePoints(int amount) { prestigePoints += amount; }
    public void removePrestigePoints(int amount) { prestigePoints -= amount; }

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

    public Tribe getTribe(){ return tribe; }

    // boolean getters
    public boolean hasShamanicImmunity() {
        return shamanicImmunity;
    }
    public boolean hasShamanicDoublePrestige() {
        return shamanicDoublePrestige;
    }
    public boolean hasShamanicBonusIcons() {
        return shamanicBonusStars;
    }
    public boolean hasExtraFoodOnTotemReturn() {
        return extraFoodOnTotemReturn;
    }
    public boolean hasExtraDraw() {
        return extraDraw;
    }

    // boolean setters
    public void setShamanicImmunity(boolean shamanicImmunity) {
        this.shamanicImmunity = shamanicImmunity;
    }
    public void setShamanicBonusIcons(boolean shamanicBonusStars) {
        this.shamanicBonusStars = shamanicBonusStars;
    }
    public void setShamanicDoublePrestige(boolean shamanicDoublePrestige) {
        this.shamanicDoublePrestige = shamanicDoublePrestige;
    }
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

    public String getName() {
        return name;
    }
    public TotemColor getColor() {
        return color;
    }
    public TotemLocation getLocation() {
        return totemLocation;
    }
}