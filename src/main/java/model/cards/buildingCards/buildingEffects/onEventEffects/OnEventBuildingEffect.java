package model.cards.buildingCards.buildingEffects.onEventEffects;

import model.cards.buildingCards.buildingEffects.BuildingEffect;
import model.player.Player;

/**
 * Base class and default no-op implementation for building effects triggered by global game events.
 * <p>
 * This class functions as an implementation of the Adapter pattern. It allows concrete subclass
 * effects (such as {@code SustenanceDiscountEffect} or {@code HunterBonusForHuntEvent}, instantiated
 * by the {@link model.factories.BuildingCardFactory}) to selectively override only the specific
 * event lifecycle hooks they react to, inheriting safe default behaviors for all other events.
 * </p>
 * * @author Vincenzo Parente
 */
public class OnEventBuildingEffect implements BuildingEffect {

    /**
     * {@inheritDoc}
     * <p>
     * For event-driven effects, this method hooks the instance into the player's tribe
     * reactive event listener registry, ensuring it intercepts phase-specific event callbacks.
     * </p>
     *
     * @param player the target {@link Player} acquiring the effect
     */
    @Override
    public void registerSelf(Player player) {
        player.getTribe().registerOnEventEffect(this);
    }

    /**
     * Callback hook executed during the resolution phase of a Sustenance event.
     * <p>
     * Concrete implementations override this method to compute and return economic modifications,
     * such as resource discounts required to feed the tribe.
     * </p>
     *
     * @param player the {@link Player} undergoing the Sustenance evaluation phase
     * @return the integer value of the resource modifier or discount (defaults to {@code 0})
     */
    public int applyOnSustenance(Player player) {
        return 0;
    }

    /**
     * Callback hook executed during the resolution phase of a Shamanic Ritual event.
     *
     * @param player the {@link Player} executing the Shamanic Ritual interactions
     */
    public void applyOnShamanicRitual(Player player) {}

    /**
     * Callback hook executed during the resolution phase of a Cave Paintings event.
     *
     * @param player the {@link Player} executing the Cave Paintings interactions
     */
    public void applyOnCavePaintings(Player player) {}

    /**
     * Callback hook executed during the resolution phase of a Hunt event.
     *
     * @param player the {@link Player} executing the Hunt interactions
     */
    public void applyOnHunt(Player player) {}
}