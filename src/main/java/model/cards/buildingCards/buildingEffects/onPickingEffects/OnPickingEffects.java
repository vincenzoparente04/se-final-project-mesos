package model.cards.buildingCards.buildingEffects.onPickingEffects;

import model.cards.buildingCards.buildingEffects.BuildingEffect;
import model.player.Player;

import java.util.function.Consumer;

/**
 * Implements an immediate execution strategy for building effects activated upon card acquisition.
 * <p>
 * This class leverages functional composition via a {@link Consumer} callback to perform
 * structural mutations directly onto the player's domain model at the exact moment the card
 * is picked. By injecting specialized behavior blocks (lambdas) via the {@link model.factories.BuildingCardFactory},
 * it seamlessly configures diverse player-scoped capabilities—such as toggling passive flags
 * (e.g., shamanic immunity, extra draws, or totem resource modifiers)—eliminating the architectural
 * overhead of maintaining individual concrete subclasses for simple state mutations.
 * </p>
 * * @author Vincenzo Parente
 */
public class OnPickingEffects implements BuildingEffect {

    /**
     * The functional callback block responsible for mutating the state configuration of the target player.
     */
    private final Consumer<Player> flagSetter;

    /**
     * Constructs a new immediate building effect initialized with a specific mutation routine.
     *
     * @param flagSetter the functional {@link Consumer} encapsulating the exact state modification
     * logic to run against the player instance
     */
    public OnPickingEffects(Consumer<Player> flagSetter) {
        this.flagSetter = flagSetter;
    }

    /**
     * <p>
     * For picking-time effects, this method immediately fires the encapsulated functional
     * callback loop, passing the acquiring player as the target argument for direct state
     * modification, rather than registering the instance into a deferred reactive event queue.
     * </p>
     *
     * @param player the target {@link Player} picking the card and undergoing immediate state modification
     */
    @Override
    public void registerSelf(Player player) {
        flagSetter.accept(player);
    }
}