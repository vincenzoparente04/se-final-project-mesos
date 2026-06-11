package model.cards.eventCards;

import model.enums.Era;
import model.enums.EventType;
import model.player.Player;
import shared.dto.event.EventResolutionDto;
import shared.dto.event.PlayerEventDeltaDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the global concrete event execution card for the Shamanic Ritual phase.
 * <p>
 * This event implements a relative performance comparison (majority vs. minority) among all active
 * participants based on their aggregate shamanic star counts. The resolution algorithm evaluates
 * player states dynamically by compounding intrinsic tribal stars with building-derived capabilities
 * (such as flat icon padding, score multiplication, and penalty mitigation flags).
 * </p>
 */
public class ShamanicRitualEventCard extends EventCard {

    /**
     * Constructs a new concrete {@code ShamanicRitualEventCard} instance.
     *
     * @param id the unique sequential identifier assigned by the factory layer
     * @param era the chronological {@link Era} this card is bound to
     * @param playerCount the minimum player threshold required to inject this card into play
     * @param imagePath the resource path for this card's front graphical asset
     * @param backImagePath the resource path for the standard event deck back asset
     */
    public ShamanicRitualEventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * <p>
     * <b>Algorithmic Resolution Pipeline:</b>
     * <ol>
     * <li>Computes the chronological scaling index: {@code eraIndex = era.ordinal() + 1}.</li>
     * <li><b>Demographic Audit:</b> Iterates through all players to map total shamanic star weights.
     * If a player possesses an active bonus icon flag ({@link Player#hasShamanicBonusIcons()}),
     * an additional padding of 3 stars is structurally added to their evaluation tally.</li>
     * <li><b>Extrema Identification:</b> Determines the absolute maximum ({@code max}) and minimum ({@code min})
     * star metrics present across the global sample.</li>
     * <li><b>Mathematical Scale Definition:</b> Defines the reward baseline as {@code eraIndex * 5} and
     * the regression penalty baseline as {@code 3 + (2 * (eraIndex - 1))}.</li>
     * <li><b>Majority Payload Allocation:</b> Awards the calculated baseline to all players matching the
     * {@code max} threshold. If a winning player has the doubling modifier active ({@link Player#hasShamanicDoublePrestige()}),
     * the prestige point payload is multiplied by 2.</li>
     * <li><b>Minority Penalty Execution:</b> Deducts the regression penalty from all players matching the
     * {@code min} threshold, provided they do not possess protective immunity ({@link Player#hasShamanicImmunity()}).</li>
     * <li><b>State Serialization:</b> Compiles data deltas and performance context labels ("tied", "majority",
     * "minority", "middle") into transportable {@link PlayerEventDeltaDto} instances.</li>
     * </ol>
     * </p>
     *
     * @param players the active list of {@link Player} instances participating in the game
     * @return a fully populated, serialized network transport object {@link EventResolutionDto} summarizing the phase outcomes
     */
    @Override
    public EventResolutionDto resolve(List<Player> players) {
        int eraIndex = this.getEra().ordinal() + 1;

        // 1. Calculates icons considering active building modifications
        Map<Player, Integer> iconCounts = new HashMap<>();
        Map<Player, Integer> prestigeBeforeMap = new HashMap<>();
        Map<Player, Integer> foodBeforeMap = new HashMap<>();
        for (Player p : players) {
            int icons = p.getTribe().getTotalShamanStars();
            if (p.hasShamanicBonusIcons()) {
                icons += 3;
            }
            iconCounts.put(p, icons);
            prestigeBeforeMap.put(p, p.getPrestigePoints());
            foodBeforeMap.put(p, p.getFood());
        }

        // 2. Finds relative global bounds
        int max = Collections.max(iconCounts.values());
        int min = Collections.min(iconCounts.values());

        // 3. Structural coefficients evaluation
        int gain = eraIndex * 5;
        int loss = 3 + (2 * (eraIndex - 1));

        // 4. Distributes rewards to the majority holders (with potential multipliers)
        players.stream()
                .filter(p -> iconCounts.get(p) == max)
                .forEach(p -> {
                    int reward = gain;
                    if (p.hasShamanicDoublePrestige()) {
                        reward *= 2;
                    }
                    p.addPrestigePoints(reward);
                });

        // 5. Deducts penalties from minority holders (with conditional immunity check)
        players.stream()
                .filter(p -> iconCounts.get(p) == min)
                .filter(p -> !p.hasShamanicImmunity())
                .forEach(p -> p.removePrestigePoints(loss));

        // 6. Packages delta metrics for network distribution
        List<PlayerEventDeltaDto> deltas = new ArrayList<>();
        for (Player p : players) {
            int icons = iconCounts.get(p);
            int foodBefore = foodBeforeMap.get(p);
            int prestigeBefore = prestigeBeforeMap.get(p);
            int foodAfter = p.getFood();
            int prestigeAfter = p.getPrestigePoints();

            String role;
            if (max == min) {
                role = "tied";
            } else if (icons == max) {
                role = "majority";
            } else if (icons == min) {
                role = p.hasShamanicImmunity() ? "minority (immune)" : "minority";
            } else {
                role = "middle";
            }

            String details = "%d icons (%s) → %+d prestige"
                    .formatted(icons, role, prestigeAfter - prestigeBefore);

            deltas.add(new PlayerEventDeltaDto(p.getName(),
                    foodBefore, foodAfter, prestigeBefore, prestigeAfter, details));
        }

        return new EventResolutionDto(
                EventType.SHAMANIC_RITUAL.name(), this.getEra().name(), this.getId(),
                "Shamanic ritual - " + this.getEra().name(),
                deltas);
    }
}