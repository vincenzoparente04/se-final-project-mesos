package model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects;

import model.cards.buildingCards.buildingEffects.BuildingEffect;
import model.player.Player;

/**
 * Abstract base class for building effects that trigger dynamically upon character card acquisition.
 * <p>
 * This class establishes a reactive event-hook pattern. When a player possesses a building with
 * this effect, the instance registers itself as a listener within the player's tribe. Whenever
 * a new character card is subsequently acquired by a player having mappings like {@code BonusForCompletedSet} or
 * {@code InventorsPairBonus}), the player's tribe intercepts the event and dispatches execution to the
 * concrete {@link #applyEffect(Player)} implementation.
 * </p>
 */
public abstract class OnAcquireBuildingEffect implements BuildingEffect {

    /**
     * <p>
     * For character acquisition effects, this method hooks the instance directly into the
     * player's tribe reactive listener registry, ensuring it intercepts future character
     * acquisition lifecycle events.
     * </p>
     *
     * @param player the target {@link Player} acquiring the effect
     */
    @Override
    public void registerSelf(Player player) {
        player.getTribe().registerOnAcquireEffect(this);
    }

    /**
     * Executes the concrete reactive behavior triggered by a character card acquisition event.
     * <p>
     * This method is invoked automatically by the system whenever a character card is added
     * to the player's tribe, allowing real-time evaluation of state conditions (such as
     * verifying newly completed sets or character pairings) to award immediate bonuses.
     * </p>
     *
     * @param player the {@link Player} whose state has mutated due to character acquisition
     * and who receives the resulting effect payoff
     */
    public abstract void applyEffect(Player player);
}