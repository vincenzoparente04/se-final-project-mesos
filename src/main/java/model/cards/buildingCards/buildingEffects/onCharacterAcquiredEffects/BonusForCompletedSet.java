package model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects;

import model.player.Player;

/**
 * Implements a reactive building effect that awards a resource payload whenever the player
 * completes a new full set of unique character cards within their tribe.
 * <p>
 * This class tracks the high-water mark of completed character sets using a stateful delta evaluation.
 * Upon acquisition, it benchmarks pre-existing sets to guarantee that only newly synthesized
 * sets trigger the economic resource reward during the character acquisition lifecycle.
 * </p>
 * * @author Vincenzo Parente
 */
public class BonusForCompletedSet extends OnAcquireBuildingEffect {

    /**
     * Stores the historical high-water mark configuration of completed sets
     * to prevent retroactive exploitation and filter incremental changes.
     */
    private int alreadyCompletedSets;

    /**
     * Constructs the effect with its display description.
     *
     * @param description the human-readable summary shown next to the building card
     */
    public BonusForCompletedSet(String description) {
        super(description);
    }

    /**
     * <p>
     * Overrides the registration lifecycle hook to perform an immediate snapshotting evaluation
     * of the player's current assets. It initializes the baseline watermark with the count
     * of completed sets at the exact moment this building enters play, then attaches the listener
     * registry reference.
     * </p>
     *
     * @param player the target {@link Player} acquiring the building effect
     */
    @Override
    public void registerSelf(Player player) {
        this.alreadyCompletedSets = player.getTribe().countCompleteSets();
        player.getTribe().registerOnAcquireEffect(this);
    }

    /**
     * Evaluates the complete set counter inside the player's tribe post-acquisition
     * to verify if a set threshold increment occurred.
     * <p>
     * If the recalculation detects that the current completed set metric strictly exceeds
     * the cached tracking variable, an incremental milestone has been reached. The player
     * is immediately credited with 5 food units, and the internal tracking baseline is adjusted
     * to the new absolute maximum.
     * </p>
     *
     * @param player the {@link Player} whose state has mutated due to character acquisition
     */
    @Override
    public void applyEffect(Player player) {
        int currentSets = player.getTribe().countCompleteSets();
        if (currentSets > alreadyCompletedSets) {
            player.addFood(5);
            alreadyCompletedSets = currentSets;
        }
    }
}
