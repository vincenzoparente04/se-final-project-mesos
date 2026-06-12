package model.cards.buildingCards.buildingEffects.onEventEffects;

import model.player.Player;
import model.player.Tribe;

import java.util.function.ToIntFunction;

/**
 * Implements an event-driven building effect that provides an economic resource mitigation
 * (food discount) during the global Sustenance event resolution phase.
 * <p>
 * This class leverages functional composition via a {@link ToIntFunction} to dynamically compute
 * the discount based on a specific character demographic within the player's tribe. This allows
 * the {@link model.factories.BuildingCardFactory} to instantiate targeted variations (e.g., discounts
 * scaled by Artist, Gatherer, or Inventor headcounts) without duplicating subclass architectures.
 * </p>
 * * @author Vincenzo Parente
 */
public class SustenanceDiscountEffect extends OnEventBuildingEffect {

    /**
     * The unit food resource discount value applied per quantified character.
     */
    private final int discountPerCharacter;

    /**
     * The functional extractor used to retrieve the specific integer demographic metric from the tribe.
     */
    private final ToIntFunction<Tribe> correctGetter;

    /**
     * Constructs a new reactive sustenance modifier with a specific discount scale and target demographic.
     *
     * @param discountPerCharacter the quantity of food resource discount granted per character unit
     * @param correctGetter the functional reference used to evaluate the applicable
     * character headcount from the player's tribe state
     * @param description the human-readable summary shown next to the building card
     */
    public SustenanceDiscountEffect(int discountPerCharacter, ToIntFunction<Tribe> correctGetter, String description) {
        super(description);
        this.discountPerCharacter = discountPerCharacter;
        this.correctGetter = correctGetter;
    }

    /**
     * <p>
     * Intercepts the Sustenance event lifecycle. This method executes the functional getter against
     * the player's tribe, multiplies the resulting headcount by the unit discount coefficient, and
     * returns the aggregate discount value. The final resource deduction arithmetic is handled and
     * resolved by the event card processing engine.
     * </p>
     *
     * @param player the {@link Player} undergoing the Sustenance evaluation phase
     * @return the total calculated integer food resource discount available to mitigate sustenance costs
     */
    @Override
    public int applyOnSustenance(Player player) {
        return correctGetter.applyAsInt(player.getTribe()) * discountPerCharacter;
    }
}