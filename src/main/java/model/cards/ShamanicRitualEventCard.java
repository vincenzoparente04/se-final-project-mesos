package model.cards;

import model.enums.CardType;
import model.player.Player;

import java.util.List;

public class ShamanicRitualEventCard extends EventCard{
    /**
     *
     * @param players
     * @implNote Il metodo assegna i punti al vincitore e li rimuove al perdente
     */
    @Override
    void resolve(List<Player> players) {
        // farla con programmazione funzionale
        int maxStars = players.stream()
                .mapToInt(p -> p.getTribe().getTotalShamanStars())
                .max()
                .orElse(0);

        int minStars = players.stream()
                .mapToInt(p -> p.getTribe().getTotalShamanStars())
                .min()
                .orElse(0);

        // funzioni ricavate dai valori delle carte
        int gain = this.getEra().ordinal() * 5;
        int loss = -3 - (2 * (this.getEra().ordinal() - 1) );

        // assegnazione prima al massimo e poi al minimo (per gestire i casi limite dell'effetto)
        players.stream()
                .filter(p -> p.getTribe().getTotalShamanStars() == maxStars)
                .forEach(p -> p.addPrestigePoints(gain));

        players.stream()
                .filter(p -> p.getTribe().getTotalShamanStars() == minStars)
                .forEach(p -> p.removePrestigePoints(loss));
    }

    @Override
    public CardType getCardType() {
        return null;
    }
}
