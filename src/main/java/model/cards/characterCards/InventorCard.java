package model.cards.characterCards;

import model.enums.Era;
import model.enums.InventionIcon;
import model.rowsManager.CardVisitor;

/**
 * Represents a concrete Character card of type Inventor within the tribe deck configuration.
 * <p>
 * Inventors are distinct demographic units characterized by an associated {@link InventionIcon}.
 * These icons serve as key structural tokens within the player's tribe state, driving aggregate
 * evaluations such as icon-pair tracking routines (e.g., triggering resource bonuses in
 * {@link model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects.InventorsPairBonus})
 * or contributing to specific end-game scoring multipliers.
 * </p>
 */
public class InventorCard extends CharacterCard {

    /**
     * The specific technological breakthrough icon categorized by this inventor card.
     */
    private final InventionIcon inventionIcon;

    /**
     * Constructs a new concrete {@code InventorCard} instance with a designated invention icon.
     *
     * @param id            the unique sequential identifier assigned by the factory layer
     * @param era           the chronological {@link Era} this card belongs to
     * @param playerCount   the minimum player threshold required to inject this card into the active deck
     * @param inventionIcon the structural {@link InventionIcon} associated with this character
     * @param imagePath     the resource path for this card's front graphical asset
     * @param backImagePath the resource path for the standard tribe deck back asset
     */
    public InventorCard(int id, Era era, int playerCount, InventionIcon inventionIcon, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
        this.inventionIcon = inventionIcon;
    }

    /**
     * Retrieves the technological icon marker bound to this inventor card.
     *
     * @return the {@link InventionIcon} enum constant
     */
    public InventionIcon getInventionIcon() {
        return inventionIcon;
    }

    /**
     * <p>
     * Enrolls this instance into the target player's demographic inventory by invoking
     * {@link model.player.Tribe#addInventor(InventorCard)}. This mutation registers the card
     * within the tribe's internal icon-mapped collections, immediately updating the state baseline
     * audited by reactive pair checking hooks and end-game score calculators.
     * </p>
     *
     * @param player the target {@link model.player.Player} whose tribe demographic state will include this card
     */
    @Override
    public void registerToTribe(model.player.Player player) {
        player.getTribe().addInventor(this);
    }

    /**
     * Routes this card to the inventor-specific overload of the visitor, completing the
     * double-dispatch so role-aware visitors receive the concrete type without casting.
     *
     * @param visitor the visitor performing an operation on this card
     */
    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}