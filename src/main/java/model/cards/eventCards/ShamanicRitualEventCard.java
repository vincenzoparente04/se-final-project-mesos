package model.cards.eventCards;

import model.enums.CardType;
import model.enums.Era;
import model.player.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShamanicRitualEventCard extends EventCard {


    public ShamanicRitualEventCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }


    /**
     *
     * @param players
     * @implNote Il metodo assegna i punti al vincitore e li rimuove al perdente
     */
    public void resolve(List<Player> players) {
        // 1. calcola icone con bonus building
        Map<Player, Integer> iconCounts = new HashMap<>();
        for (Player p : players) {
            int icons = p.getTribe().getTotalShamanStars();
            if (p.hasShamanicBonusIcons()) {
                icons += 3;
            }
            iconCounts.put(p, icons);
        }

        // 2. determina maggioranza e minoranza
        int max = Collections.max(iconCounts.values());
        int min = Collections.min(iconCounts.values());

        // 3. funzioni ricavate dai valori delle carte
        int gain = this.getEra().ordinal() * 5;
        int loss = -3 - (2 * (this.getEra().ordinal() - 1));

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

        // 5. rimuovi punti al minimo (se diverso dal massimo)
        players.stream()
                .filter(p -> iconCounts.get(p) == min && min != max)
                .filter(p -> !p.hasShamanicImmunity())
                .forEach(p -> p.removePrestigePoints(loss));
    }
}
