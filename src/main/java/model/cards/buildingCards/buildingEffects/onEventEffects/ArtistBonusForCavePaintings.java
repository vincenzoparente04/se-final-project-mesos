package model.cards.buildingCards.buildingEffects.onEventEffects;

import model.player.Player;

/**
 * Represents an event-driven building effect that hooks into the global Cave Paintings event phase.
 * <p>
 * This class applies an economic resource reward (food) that scales linearly based on the demographic
 * headcount of artist characters present in the player's tribe when the event resolves.
 * The scaling coefficient is configured dynamically via the {@link model.factories.BuildingCardFactory}
 * during factory instantiation.
 * </p>
 * * @author Vincenzo Parente
 */
public class ArtistBonusForCavePaintings extends OnEventBuildingEffect {

    /**
     * The linear scaling coefficient used to compute the food unit payout based on the artist headcount.
     */
    private final int foodMultiplier;

    /**
     * Constructs a new reactive event modifier with a specific scaling configuration.
     *
     * @param foodMultiplier the amount of food units granted per artist character inside the tribe
     * @param description the human-readable summary shown next to the building card
     */
    public ArtistBonusForCavePaintings(int foodMultiplier, String description){
        super(description);
        this.foodMultiplier = foodMultiplier;
    }

    /**
     * <p>
     * Intercepts the Cave Paintings event lifecycle. This method interrogates the player's tribe
     * to fetch the current artist count, calculates the total resource yield by applying the internal
     * multiplier, and directly updates the player's food resource inventory.
     * </p>
     *
     * @param player the {@link Player} executing the Cave Paintings event phase
     */
    @Override
    public void applyOnCavePaintings(Player player) {
        player.addFood(player.getTribe().getArtistCount() * foodMultiplier);
    }
}