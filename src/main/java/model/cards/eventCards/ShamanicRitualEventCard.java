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
     * @implNote Each player counts the number of shamanic icons they have (including those from buildings).
     * The player(s) with the most icons gain prestige points equal to 5 times the era of the card and manages eventual
     * ties. The method also considers the possible effects given by buildings.
     * @param players
     */
    public EventResolutionDto resolve(List<Player> players) {
        int eraIndex = this.getEra().ordinal() + 1;

        // 1. calcola icone con bonus building
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

        // 2. determina maggioranza e minoranza
        int max = Collections.max(iconCounts.values());
        int min = Collections.min(iconCounts.values());

        // 3. funzioni ricavate dai valori delle carte
        int gain = eraIndex * 5;
        int loss = 3 + (2 * (eraIndex - 1));

        // 4. assegna punti al massimo
        players.stream()
                .filter(p -> iconCounts.get(p) == max)
                .forEach(p -> {
                    int reward = gain;
                    if (p.hasShamanicDoublePrestige()) {
                        reward *= 2;
                    }
                    p.addPrestigePoints(reward);
                });

        // 5. rimuovi punti al minimo (solo se diverso dal massimo, cioè non pareggio totale)
        players.stream()
                .filter(p -> iconCounts.get(p) == min)
                .filter(p -> min != max)
                .filter(p -> !p.hasShamanicImmunity())
                .forEach(p -> p.removePrestigePoints(loss));

        // 6. costruisci i delta per il client
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
