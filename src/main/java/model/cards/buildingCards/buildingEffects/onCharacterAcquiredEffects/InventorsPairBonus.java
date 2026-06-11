package model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects;

import model.cards.characterCards.InventorCard;
import model.enums.InventionIcon;
import model.player.Player;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements a reactive building effect that awards a resource bonus whenever a new pair
 * of matching invention icons is formed within the player's tribe.
 * <p>
 * The class maintains an internal state configuration using a delta-tracking mechanism. Upon
 * acquisition, it snapshots pre-existing paired sets to prevent retroactive exploitation.
 * During subsequent character acquisitions, it leverages the atomic behavior of {@link Set#add(Object)}
 * to simultaneously check for eligibility and update state in a single short-circuit operations pipeline.
 * </p>
 */
public class InventorsPairBonus extends OnAcquireBuildingEffect {

    /**
     * Tracks the collection of unique {@link InventionIcon} constants that have already reached
     * or exceeded a paired threshold (2 or more cards), serving as the evaluation exclusion filter.
     */
    private Set<InventionIcon> alreadyPaired;

    /**
     * <p>
     * Overrides the registration lifecycle hook to perform an initial structural evaluation
     * of the player's current assets. It populates the exclusion filter with any invention icon
     * groups that already contain a pair or more at the exact moment this building is acquired,
     * ensuring bonuses are only granted for newly established pairs moving forward.
     * </p>
     *
     * @param player the target {@link Player} acquiring the building effect
     */
    @Override
    public void registerSelf(Player player) {
        // fotografa le coppie al momento dell'acquisto
        this.alreadyPaired = EnumSet.noneOf(InventionIcon.class);
        for (Map.Entry<InventionIcon, List<InventorCard>> entry
                : player.getTribe().getInventorsByIcon().entrySet()) {
            if (entry.getValue().size() >= 2) {
                alreadyPaired.add(entry.getKey());
            }
        }
        player.getTribe().registerOnAcquireEffect(this);
    }

    /**
     * Evaluates the player's inventor inventory following a character card acquisition event
     * to detect newly completed icon pairings.
     * <p>
     * <b>Technical Logic Detail:</b> The conditional statement evaluates {@code alreadyPaired.add(entry.getKey())}.
     * Since {@link Set#add(Object)} returns {@code true} only if the element was <i>not already present</i>
     * in the collection, this operation executes a thread-safe, atomic check-and-act routine: it verifies
     * that the icon group has just reached the threshold for the first time, and immediately seals it against
     * future evaluations. If a new pair is detected, the player is awarded 3 food units.
     * </p>
     *
     * @param player the {@link Player} whose state has mutated due to character acquisition
     */
    @Override
    public void applyEffect(Player player) {
        for (Map.Entry<InventionIcon, List<InventorCard>> entry
                : player.getTribe().getInventorsByIcon().entrySet()) {
            if (entry.getValue().size() >= 2 && alreadyPaired.add(entry.getKey())) {
                player.addFood(3);
                return;
            }
        }
    }
}
