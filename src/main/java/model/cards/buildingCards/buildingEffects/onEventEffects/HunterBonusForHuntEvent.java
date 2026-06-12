package model.cards.buildingCards.buildingEffects.onEventEffects;

import model.player.Player;

/**
 * Represents a specific event-driven building effect that hooks into the global Hunt event phase.
 * <p>
 * This class calculates and applies linear scaling rewards (both food resources and prestige points)
 * proportional to the demographic headcount of hunter characters present within the player's tribe
 * at the moment of the event execution. The respective scaling coefficients are decoupled and injected
 * via the {@link model.factories.BuildingCardFactory} during structural initialization.
 * </p>
 * * @author Vincenzo Parente
 */
public class HunterBonusForHuntEvent extends OnEventBuildingEffect {

    /**
     * The linear scaling coefficient used to compute prestige points based on the hunter headcount.
     */
    private final int prestigeMultiplier;

    /**
     * The linear scaling coefficient used to compute food units based on the hunter headcount.
     */
    private final int foodMultiplier;

    /**
     * Constructs a new reactive configuration with specific multiplier coefficients.
     *
     * @param prestigeMultiplier the amount of prestige points granted per hunter character
     * @param foodMultiplier the amount of food units granted per hunter character
     * @param description the human-readable summary shown next to the building card
     */
    public HunterBonusForHuntEvent(int prestigeMultiplier, int foodMultiplier, String description) {
        super(description);
        this.foodMultiplier = foodMultiplier;
        this.prestigeMultiplier = prestigeMultiplier;
    }

    /**
     * <p>
     * Intercepts the Hunt event lifecycle. This method fetches the current count of hunter characters
     * from the player's tribe, evaluates the compound yields by applying the internal multipliers,
     * and instantly mutates the target player's resource pool and prestige score.
     * </p>
     *
     * @param player the {@link Player} executing the Hunt event phase
     */
    @Override
    public void applyOnHunt(Player player) {
        int hunterCount = player.getTribe().getHunterCount();
        player.addFood(hunterCount * foodMultiplier);
        player.addPrestigePoints(hunterCount * prestigeMultiplier);
    }
}