package model.cards.characterCards;

import model.enums.Era;
import model.player.Player;
import model.rowsManager.CardVisitor;

/**
 * Represents a concrete Character card of type Hunter within the tribe deck configuration.
 * <p>
 * Hunters serve a critical dual role: expanding the tribe's hunter demographic for event-driven
 * scaling benefits (such as phase bonuses handled via {@link model.cards.buildingCards.buildingEffects.onEventEffects.HunterBonusForHuntEvent})
 * and potentially triggering immediate economic payoffs upon entry. Instances are initialized
 * and structurally configured with specific trigger flags by the {@link model.factories.TribeCardFactory}.
 * </p>
 */
public class HunterCard extends CharacterCard {

    /**
     * Indicates whether this specific hunter card possesses an entry execution icon,
     * which fires an immediate cascading resource bonus upon being acquired.
     */
    private final boolean triggerIcon;

    /**
     * Constructs a new concrete {@code HunterCard} instance with a specified entry trigger configuration.
     *
     * @param id the unique sequential identifier assigned by the factory layer
     * @param era the chronological {@link Era} this card belongs to
     * @param playerCount the minimum player threshold required to inject this card into the active deck
     * @param hasTriggerIcon {@code true} if this card features an immediate entry resource bonus, {@code false} otherwise
     * @param imagePath the resource path for this card's front graphical asset
     * @param backImagePath the resource path for the standard tribe deck back asset
     */
    public HunterCard(int id, Era era, int playerCount, boolean hasTriggerIcon, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
        this.triggerIcon = hasTriggerIcon;
    }

    /**
     * <p>
     * Enrolls this instance into the target player's demographic inventory by invoking
     * {@link model.player.Tribe#addHunter(HunterCard)}. Additionally, implements an immediate
     * cascading trigger condition: if {@link #triggerIcon} evaluates to {@code true}, it
     * interrogates the newly updated hunter population count and instantly credits the player's
     * inventory with an equivalent amount of food resources.
     * </p>
     *
     * @param player the target {@link Player} whose tribe demographic state will include this card
     */
    @Override
    public void registerToTribe(Player player) {
        player.getTribe().addHunter(this);

        if (triggerIcon) {
            player.addFood(player.getTribe().getHunterCount());
        }
    }

    /**
     * Determines whether this hunter card activates an immediate economic bonus upon acquisition.
     *
     * @return {@code true} if the entry trigger icon is present, {@code false} otherwise
     */
    public boolean hasTriggerIcon() {
        return triggerIcon;
    }

    /**
     * Routes this card to the hunter-specific overload of the visitor, completing the
     * double-dispatch so role-aware visitors receive the concrete type without casting.
     *
     * @param visitor the visitor performing an operation on this card
     */
    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}