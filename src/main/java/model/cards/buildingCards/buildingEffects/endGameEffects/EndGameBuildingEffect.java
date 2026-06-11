package model.cards.buildingCards.buildingEffects.endGameEffects;

import model.cards.buildingCards.buildingEffects.BuildingEffect;
import model.player.Player;
import model.player.Tribe;

import java.util.function.ToIntFunction;

/**
 * Implements a deferred end-game scoring strategy for building cards.
 * <p>
 * Instead of hardcoding distinct subclasses for every possible scoring condition, this class
 * relies on functional composition via {@link ToIntFunction} to dynamically evaluate a player's
 * {@link Tribe} state. By injecting specific method references (e.g., {@code Tribe::getHunterCount},
 * {@code Tribe::countCompleteSets}) or custom lambdas (e.g., {@code tribe -> 1} for flat points)
 * alongside a numeric multiplier, the factory can seamlessly instantiate highly versatile and
 * complex end-game point calculators.
 * </p>
 */
public class EndGameBuildingEffect implements BuildingEffect {

    private final int multiplier;
    private final ToIntFunction<Tribe> getter;

    /**
     * Constructs a new deferred end-game scoring effect based on a specific demographic metric.
     *
     * @param multiplier the prestige point multiplier awarded for each unit returned by the getter
     * @param getter the functional interface used to dynamically extract the relevant integer
     * metric (e.g., number of characters, completed sets) from the player's tribe
     */
    public EndGameBuildingEffect(int multiplier, ToIntFunction<Tribe> getter) {
        this.multiplier = multiplier;
        this.getter = getter;
    }

    /**
     * <p>
     * For end-game effects, this method defers execution by registering the instance into
     * the player's internal end-game evaluation queue, ensuring it is only triggered during
     * the final scoring phase.
     * </p>
     *
     * @param player the target {@link Player} acquiring the effect
     */
    @Override
    public void registerSelf(Player player) {
        player.getTribe().registerEndGameEffect(this);
    }

    /**
     * Executes the deferred scoring logic, evaluating the functional getter against the
     * player's current tribe state, and immediately updates the player's prestige score.
     *
     * @param player the target {@link Player} receiving the calculated prestige points
     */
    public void applyEffect(Player player) {
        player.addPrestigePoints(multiplier * getter.applyAsInt(player.getTribe()));
    }
}