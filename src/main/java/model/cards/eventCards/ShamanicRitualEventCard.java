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

public class ShamanicRitualEventCard extends EventCard {
    public ShamanicRitualEventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * Each player's shamanic icon count is tallied, including bonuses from buildings.
     * The player(s) with the most icons gain prestige equal to 5 times the era index;
     * those with the fewest lose a scaled amount unless they hold shamanic immunity.
     * Building effects that double the prestige reward are applied before awarding points.
     * Ties at majority or minority are handled correctly.
     *
     * @param players the list of active players
     * @return an {@link EventResolutionDto} summarising the per-player outcome
     */
    public EventResolutionDto resolve(List<Player> players) {
        int eraIndex = this.getEra().ordinal() + 1;

        // 1. calculates icons considering building cards
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

        // 2. finds max and min
        int max = Collections.max(iconCounts.values());
        int min = Collections.min(iconCounts.values());

        // 3. functions found in the cards
        int gain = eraIndex * 5;
        int loss = 3 + (2 * (eraIndex - 1));

        // 4. gives points to who has max
        players.stream()
                .filter(p -> iconCounts.get(p) == max)
                .forEach(p -> {
                    int reward = gain;
                    if (p.hasShamanicDoublePrestige()) {
                        reward *= 2;
                    }
                    p.addPrestigePoints(reward);
                });

        // 5. removes from who has minimum (if not same)
        players.stream()
                .filter(p -> iconCounts.get(p) == min)
                .filter(p -> !p.hasShamanicImmunity())
                .forEach(p -> p.removePrestigePoints(loss));

        // 6. builds deltas to be shown at the clients
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
