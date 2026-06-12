package model.cards.characterCards;

import model.enums.Era;
import model.player.Player;
import model.rowsManager.CardVisitor;

/**
 * Represents a concrete Character card of type Builder within the tribe deck configuration.
 * <p>
 * Builders provide vital economic and victory conditions to the player. Each active builder card
 * in a tribe reduces the food cost of future {@link model.cards.buildingCards.BuildingCard} acquisitions
 * via an internal discount accumulator, while also contributing direct prestige points to the player's score.
 * </p>
 */
public class BuilderCard extends CharacterCard {

    /**
     * The structural food resource discount value provided by this specific builder
     * during a building construction transaction.
     */
    private final int builderDiscount;

    /**
     * The flat prestige point yield inherently awarded to the owner of this card.
     */
    private final int prestigePoints;

    /**
     * Constructs a new concrete {@code BuilderCard} instance with specific economic and prestige attributes.
     *
     * @param id the unique sequential identifier assigned by the factory layer
     * @param era the chronological {@link Era} this card belongs to
     * @param playerCount the minimum player threshold required to inject this card into the active deck
     * @param buildingDiscount the food resource mitigation value applied during building construction
     * @param pp the static prestige points inherently provided by this character
     * @param imagePath the resource path for this card's front graphical asset
     * @param backImagePath the resource path for the standard tribe deck back asset
     */
    public BuilderCard(int id, Era era, int playerCount, int buildingDiscount, int pp, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
        this.builderDiscount = buildingDiscount;
        this.prestigePoints = pp;
    }

    /**
     * Retrieves the static prestige points inherently yielded by this builder card.
     *
     * @return the integer prestige point value
     */
    public int getPrestigePoints() {
        return prestigePoints;
    }

    /**
     * Retrieves the food resource mitigation value supplied by this builder card.
     *
     * @return the integer transaction discount amount
     */
    public int getBuilderDiscount(){
        return builderDiscount;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Enrolls this instance into the target player's demographic inventory by invoking
     * {@link model.player.Tribe#addBuilder(BuilderCard)}. This mutation dynamically updates the
     * builder population count and aggregate transaction discounts, satisfying requirements for
     * building cost reductions and builder-centric end-game evaluation matrices.
     * </p>
     *
     * @param player the target {@link Player} whose tribe demographic state will include this card
     */
    @Override
    public void registerToTribe(Player player) {
        player.getTribe().addBuilder(this);
    }

    /**
     * Routes this card to the builder-specific overload of the visitor, completing the
     * double-dispatch so role-aware visitors receive the concrete type without casting.
     *
     * @param visitor the visitor performing an operation on this card
     */
    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}