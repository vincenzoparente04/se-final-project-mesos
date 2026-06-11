package model.cards.characterCards;

import model.rowsManager.CardVisitor;
import model.cards.TribeCard;
import model.enums.Era;

/**
 * Abstract base class for all character-type tribe cards within the game engine.
 * <p>
 * This class establishes the foundational demographic structure for the various specialized
 * tribe member roles (e.g., Artist, Builder, Gatherer, Hunter, Inventor, Shaman).
 * Extracted and initialized via the {@link model.factories.TribeCardFactory}, concrete
 * subclasses override the registration lifecycle to strictly increment category-specific
 * population counters and trigger event hooks within the player's tribe entity.
 * </p>
 */
public abstract class CharacterCard extends TribeCard {

    /**
     * Constructs a new base {@code CharacterCard} with essential structural and resource assets.
     *
     * @param id the unique sequential identifier assigned by the tribe deck factory
     * @param era the chronological {@link Era} this character card is associated with
     * @param playerCount the minimum player threshold required to inject this character into play
     * @param imagePath the resource path for the character's front graphical asset
     * @param backImagePath the resource path for the standard tribe deck back graphical asset
     */
    public CharacterCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * <p>
     * Routes the execution path directly to the character card specialization of the visitor.
     * This ensures type-safe double-dispatch processing by row management or game rule evaluation
     * sub-systems without necessitating manual casting operations.
     * </p>
     *
     * @param visitor the {@link CardVisitor} performing structural evaluations or operations on this card
     */
    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}