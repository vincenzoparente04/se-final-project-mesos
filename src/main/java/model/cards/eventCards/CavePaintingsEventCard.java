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
 * Represents the global concrete event execution card for the Cave Paintings phase.
 * <p>
 * This event class implements a conditional scoring and penalty evaluation matrix scaled by
 * the chronological progression of the game. It benchmarks the player's artist population against
 * a dynamic threshold derived from the 1-based index of the current {@link Era}.
 * Following the baseline evaluation, it performs a dispatch loop to invoke secondary reactive
 * enhancements registered within the player's active building infrastructure.
 * </p>
 */
public class CavePaintingsEventCard extends EventCard {

    /**
     * Constructs a new concrete {@code CavePaintingsEventCard} instance.
     *
     * @param id the unique sequential identifier assigned by the factory layer
     * @param era the chronological {@link Era} this card is bound to
     * @param playerCount the minimum player threshold required to inject this card into play
     * @param imagePath the resource path for this card's front graphical asset
     * @param backImagePath the resource path for the standard event deck back asset
     */
    public CavePaintingsEventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * <p>
     * <b>Algorithmic Resolution Pipeline:</b>
     * <ol>
     * <li>Computes the current environmental threshold coefficient: {@code eraIndex = era.ordinal() + 1}.</li>
     * <li>Iterates through each active game participant to track resource and score deltas.</li>
     * <li>Evaluates demographic eligibility: if a player's total artist count falls strictly below the
     * computed {@code eraIndex}, a flat penalty of 2 prestige points is deducted. Otherwise, a compounding
     * bonus equal to {@code eraIndex * artistCount} is credited.</li>
     * <li>Dispatches lifecycle callbacks to all registered {@link OnEventBuildingEffect} instances
     * intercepting the {@link OnEventBuildingEffect#applyOnCavePaintings(Player)} hook to apply building-specific yields.</li>
     * <li>Snapshots the final state and packages individual deltas into client-bound {@link PlayerEventDeltaDto} data wrappers.</li>
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
            int artists = p.getTribe().getArtistCount();

            // Threshold evaluation check against the current era standard
            if (artists < eraIndex) {
                p.removePrestigePoints(2);
            } else {
                p.addPrestigePoints(eraIndex * artists);
            }

            // Polling and triggering active building listeners
            for (OnEventBuildingEffect effect : p.getTribe().getOnEventBuildingEffects()) {
                effect.applyOnCavePaintings(p);
            }

            int foodAfter = p.getFood();
            int prestigeAfter = p.getPrestigePoints();
            String details = "%d artists (req %d) → %+d prestige"
                    .formatted(artists, eraIndex, prestigeAfter - prestigeBefore);

            deltas.add(new PlayerEventDeltaDto(p.getName(), foodBefore, foodAfter, prestigeBefore, prestigeAfter, details));
        }

        return new EventResolutionDto(EventType.CAVE_PAINTINGS.name(), this.getEra().name(), this.getId(),
                "Cave paintings - " + this.getEra().name(),
                deltas);
    }
}