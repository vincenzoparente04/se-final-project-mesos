package model.cards.buildingCards.buildingEffects;

import model.player.Player;

/**
 * Defines the contract for the functional effects associated with building cards.
 * <p>
 * Implementing classes encapsulate specific mechanical behaviors—such as immediate
 * resource bonuses, permanent passive abilities, event-driven modifiers, or end-game
 * scoring multipliers. This interface heavily utilizes the Strategy/Command pattern
 * to dynamically inject these rules into the player's game state upon acquisition.
 * </p>
 */
public interface BuildingEffect {

    /**
     * Binds and executes the specific mechanical payload of the building card onto the target player.
     * <p>
     * Depending on the concrete implementation, this method may immediately mutate player attributes
     * (e.g., setting a boolean flag for an active ability), register event-driven hooks, or attach
     * deferred end-game scoring calculators to the player's domain.
     * </p>
     *
     * @param player the target {@link Player} who acquired the associated building card and
     * to whom the effect must be applied
     */
    public void registerSelf(Player player);

    /**
     * Returns a short, human-readable summary of what this effect does, suitable for
     * display next to the building card. The text is supplied at construction time, so a
     * single parameterized effect class can describe each of its concrete configurations.
     *
     * @return the effect's display description (never {@code null})
     */
    String getDescription();
}