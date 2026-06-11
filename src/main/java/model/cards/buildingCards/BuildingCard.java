package model.cards.buildingCards;

import model.cards.Card;
import model.cards.buildingCards.buildingEffects.BuildingEffect;
import model.enums.Era;
import model.player.Player;
import model.rowsManager.CardVisitor;

/**
 * Represents a specific type of card that allows players to construct buildings.
 * <p>
 * Building cards carry a base economic cost (food) and grant both static end-game
 * prestige points and dynamic gameplay effects. This class delegates its actual
 * mechanical impact on the game state to an injected {@link BuildingEffect} implementation.
 * </p>
 */
public class BuildingCard extends Card {
    private final int foodCost;
    private final int endGamePoints;
    private final BuildingEffect effect;
    private final String effectId;          // used to specify the effect of the card in the cli

    /**
     * Constructs a new {@code BuildingCard} with the specified structural, economic, and functional attributes.
     *
     * @param id the unique sequential identifier assigned by the factory
     * @param era the chronological {@link Era} this card belongs to
     * @param playercount the minimum number of players required to include this card in the active game deck
     * @param foodCost the base amount of food resources required to acquire this building
     * @param endGamePoints the flat prestige points awarded to the owner at the end of the game
     * @param effect the instantiated polymorphic {@link BuildingEffect} bound to this card
     * @param effectId the string identifier of the effect, primarily used for CLI rendering and debugging
     * @param imagePath the resource path for the card's front graphical asset
     * @param backImagePath the resource path for the card's back graphical asset
     */
    public BuildingCard(int id, Era era, int playercount, int foodCost, int endGamePoints, BuildingEffect effect, String effectId, String imagePath, String backImagePath) {
        super(id, era, playercount, imagePath, backImagePath);
        this.foodCost = foodCost;
        this.endGamePoints = endGamePoints;
        this.effect = effect;
        this.effectId      = effectId;
    }

    /**
     * Retrieves the base economic resource cost of this building.
     *
     * @return the unmitigated food cost
     */
    public int getFoodCost() { return foodCost; }

    /**
     * Retrieves the base static prestige points awarded by this building upon game completion.
     *
     * @return the integer amount of end-game points
     */
    public int getEndGamePoints() { return endGamePoints; }

    /**
     * Retrieves the functional effect payload assigned to this building.
     *
     * @return the polymorphic {@link BuildingEffect} instance
     */
    public BuildingEffect getEffect() { return effect; }

    /**
     * Retrieves the structural string identifier of the card's effect.
     *
     * @return the textual effect ID
     */
    public String getEffectId()      { return effectId; }

    /**
     * <p>
     * For a {@code BuildingCard}, this operation explicitly adds the instance to the
     * player's building collection and triggers {@link BuildingEffect#registerSelf(Player)}
     * to bind the functional rules (immediate, event-driven, or end-game) to the player's state.
     * </p>
     *
     * @param player the target {@link Player} acquiring and paying for the building
     */
    @Override
    public void registerToTribe(Player player) {
        player.getTribe().addBuilding(this);
        this.effect.registerSelf(player);
    }

    /**
     * Calculates the dynamic food cost of this building card relative to a specific player's state.
     * <p>
     * The final transaction cost is determined by subtracting the player's total accumulated
     * builder discounts from the building's base food cost. The resulting cost is clamped to
     * a strictly non-negative integer.
     * </p>
     *
     * @param player the {@link Player} attempting to calculate the purchase cost
     * @return the fully discounted, non-negative food cost required to acquire the building
     */
    public int getDiscountedCost(Player player) {
        int buildersDiscount = player.getTribe().getTotalBuilderDiscount();
        return Math.max(0, this.foodCost - buildersDiscount);
    }

    /**
     * <p>
     * Routes the visitor explicitly to the building card execution path, ensuring type-safe
     * double-dispatch resolution without the need for manual type casting.
     * </p>
     *
     * @param visitor the {@link CardVisitor} performing an operation on this card
     */
    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}

