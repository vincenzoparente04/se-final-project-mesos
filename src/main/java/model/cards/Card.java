package model.cards;

import model.GameModel;
import model.enums.Era;
import model.player.Player;
import model.rowsManager.CardVisitor;

/**
 * Abstract base class representing a generic game card.
 * Acts as the foundational data structure for all polymorphic card hierarchies
 * (e.g., Tribe cards, Building cards) instantiated via the factory layer.
 */
public abstract class Card {
    private final int id;
    private final Era era;           // ERA_I, ERA_II, ERA_III
    private final int playerCount;   // minimum number of players required to have this card in the game (some cards are only used in games with 3, 4 or 5 players)
    private final String imagePath;
    private final String backImagePath;

    /**
     * Constructs a new base {@code Card} with the specified structural and visual properties.
     *
     * @param id the unique sequential identifier assigned by the respective factory
     * @param era the chronological {@link Era} (ERA_I, ERA_II, ERA_III) this card belongs to
     * @param playerCount the minimum number of players required for this card to be included in the game deck
     * @param imagePath the relative resource path for the card's front graphical asset
     * @param backImagePath the relative resource path for the card's back graphical asset
     */
    public Card(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        this.id = id;
        this.era = era;
        this.playerCount = playerCount;
        this.imagePath = imagePath;
        this.backImagePath = backImagePath;
    }

    /**
     * Retrieves the unique identifier of this card.
     *
     * @return the integer ID of the card
     */
    public int getId() {return id;}

    /**
     * Retrieves the chronological game era associated with this card.
     *
     * @return the {@link Era} enum constant
     */
    public Era getEra() {
        return era;
    }

    /**
     * Retrieves the minimum player threshold required to inject this card into the active game session.
     *
     * @return the minimum player count (e.g., 3, 4, or 5)
     */
    public int getMinPlayerCount() {
        return playerCount;
    }

    /**
     * Retrieves the resource path for the card's front visual representation.
     *
     * @return a string containing the image filename/path
     */
    public String getImagePath() {
        return imagePath;
    }

    /**
     * Retrieves the resource path for the card's back visual representation.
     *
     * @return a string containing the back image filename/path
     */
    public String getBackImagePath() {
        return backImagePath;
    }

    /**
     * Integrates the card into the specified player's tribe.
     * <p>
     * This method is responsible for applying state mutations when the card is acquired.
     * Depending on the concrete subtype, this triggers the specific functional interfaces
     * resolved by the factories during initialization (such as executing a dynamically bound
     * {@code BuildingEffect} or adding a specific character to the player's demographic counts).
     * </p>
     *
     * @param player the target {@link Player} acquiring the card
     */
    public abstract void registerToTribe(Player player);

    /**
     * Accepts a generic card visitor to implement double-dispatch polymorphism.
     * Used primarily by the row management system to safely route cards to their specific
     * zone or logic handler without relying on {@code instanceof} checks.
     *
     * @param visitor the {@link CardVisitor} performing an operation on this card
     */
    public abstract void accept(CardVisitor visitor);
}
