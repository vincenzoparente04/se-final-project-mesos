package model.cards;

import model.rowsManager.CardVisitor;
import model.enums.Era;

/**
 * Abstract base class representing a card belonging to the main Tribe deck.
 * <p>
 * This class acts as a primary categorical grouping for all deck-based cards, strictly
 * branching into two main polymorphic families: character cards (e.g., Hunters, Shamans,
 * Builders) and event cards. Instances of its concrete subclasses are parsed, instantiated,
 * and segregated by the {@link model.factories.TribeCardFactory} during game initialization.
 * </p>
 * * @author Vincenzo Parente
 */
public abstract class TribeCard extends Card {

    /**
     * Constructs a new {@code TribeCard} with the specified core structural and visual attributes.
     *
     * @param id the unique sequential identifier assigned by the factory
     * @param era the chronological {@link Era} this card belongs to
     * @param playerCount the minimum number of players required to include this card in the active game deck
     * @param imagePath the resource path for the card's front graphical asset
     * @param backImagePath the resource path for the card's back graphical asset (e.g., specific era or final event back)
     */
    public TribeCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }
}