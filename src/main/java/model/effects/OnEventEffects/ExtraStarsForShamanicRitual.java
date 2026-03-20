package model.effects.OnEventEffects;

import model.player.Player;

public class ExtraStarsForShamanicRitual extends OnEventEffect {
    @Override
    public void applyOnShamanicRitual(Player player) {
        // TODO da fare che nella logica della event card aggiunge 3 stelle
        // Durante l'era del cinghiale bianco gli sciamani non possono cambiare colore
        // Se il player possiede sia la carta orso che la carta iena guadagna il doppio dei punti arrosticino
    }
}
