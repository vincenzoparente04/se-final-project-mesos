package model.cards.characterCards;

import model.enums.Era;

/**
 * Represents a concrete Character card of type Gatherer within the tribe deck configuration.
 * <p>
 * Gatherers are specialized tribe members that frequently interface with automated sustenance
 * modifiers (e.g., granting resource discount structures during Sustenance event phases via
 * {@link model.cards.buildingCards.buildingEffects.onEventEffects.SustenanceDiscountEffect})
 * or triggering demographic-based end-game scoring multipliers.
 * </p>
 */
public class GathererCard extends CharacterCard {

    /**
     * Constructs a new concrete {@code GathererCard} instance.
     *
     * @param id the unique sequential identifier assigned by the factory layer
     * @param era the chronological {@link Era} this card belongs to
     * @param playerCount the minimum player threshold required to inject this card into the active deck
     * @param imagePath the resource path for this card's front graphical asset
     * @param backImagePath the resource path for the standard tribe deck back asset
     */
    public GathererCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * <p>
     * Enrolls this instance into the target player's demographic inventory by invoking
     * {@link model.player.Tribe#addGatherer(GathererCard)}. This mutation dynamically updates the
     * gatherer population metrics, satisfying prerequisites for reactive event hooks, sustenance
     * cost reductions, and demographic end-game scoring evaluations.
     * </p>
     *
     * @param player the target {@link model.player.Player} whose tribe demographic state will include this card
     */
    @Override
    public void registerToTribe(model.player.Player player) {
        player.getTribe().addGatherer(this);
    }
}