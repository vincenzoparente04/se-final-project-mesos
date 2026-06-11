package model.cards.eventCards;

import model.cards.buildingCards.buildingEffects.onEventEffects.OnEventBuildingEffect;
import model.enums.Era;
import model.enums.EventType;
import model.player.Player;
import shared.dto.event.EventResolutionDto;
import shared.dto.event.PlayerEventDeltaDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the global concrete event execution card for the Hunt phase.
 * <p>
 * This event class evaluates a direct linear scaling payoff based on the current population of
 * hunter characters within each player's tribe. Food generation remains constant per unit, whereas
 * prestige point yields are progressively scaled by a dynamic environmental multiplier derived
 * from the active {@link Era}. After the baseline rewards are computed, the class dispatches
 * execution to any reactive building extensions bound to the event hook.
 * </p>
 */
public class HuntEventCard extends EventCard {

    /**
     * Constructs a new concrete {@code HuntEventCard} instance.
     *
     * @param id the unique sequential identifier assigned by the factory layer
     * @param era the chronological {@link Era} this card is bound to
     * @param playerCount the minimum player threshold required to inject this card into play
     * @param imagePath the resource path for this card's front graphical asset
     * @param backImagePath the resource path for the standard event deck back asset
     */
    public HuntEventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * <p>
     * <b>Algorithmic Resolution Pipeline:</b>
     * <ol>
     * <li>Computes the chronological scaling index: {@code eraIndex = era.ordinal() + 1} (e.g., 1 for ERA_I, 2 for ERA_II, 3 for ERA_III).</li>
     * <li>Iterates through each active game participant to evaluate state metrics.</li>
     * <li>Applies linear primary yields: directly credits the player with 1 food unit per hunter, and awards prestige points equivalent to {@code hunterCount * eraIndex}.</li>
     * <li>Dispatches lifecycle callbacks to all registered {@link OnEventBuildingEffect} instances
     * executing the {@link OnEventBuildingEffect#applyOnHunt(Player)} hook to resolve building-specific bonuses.</li>
     * <li>Snapshots mutated resource pools and compiles the delta metrics into a client-bound {@link PlayerEventDeltaDto} wrapper.</li>
     * </ol>
     * </p>
     *
     * @param players the active list of {@link Player} instances participating in the game
     * @return a fully populated, serialized network transport object {@link EventResolutionDto} summarizing the phase outcomes
     */
    @Override
    public EventResolutionDto resolve(List<Player> players) {
        int eraIndex = this.getEra().ordinal() + 1;
        List<PlayerEventDeltaDto> deltas = new ArrayList<>();

        for (Player p : players) {
            int foodBefore = p.getFood();
            int prestigeBefore = p.getPrestigePoints();
            int hunters = p.getTribe().getHunterCount();

            // Allocation of core resource and victory point payloads
            p.addFood(hunters);
            p.addPrestigePoints(hunters * eraIndex);

            // Polling and triggering active building listeners
            for (OnEventBuildingEffect effect : p.getTribe().getOnEventBuildingEffects()) {
                effect.applyOnHunt(p);
            }

            int foodAfter = p.getFood();
            int prestigeAfter = p.getPrestigePoints();
            String details = "%d hunters → %+d food, %+d prestige"
                    .formatted(hunters, foodAfter - foodBefore, prestigeAfter - prestigeBefore);

            deltas.add(new PlayerEventDeltaDto(p.getName(), foodBefore, foodAfter, prestigeBefore, prestigeAfter, details));
        }

        return new EventResolutionDto(EventType.HUNT.name(), this.getEra().name(), this.getId(),
                "Hunt event - " + this.getEra().name(), deltas);
    }
}