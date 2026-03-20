package model.effects.OnEventEffects;

import model.effects.EndGameEffects.EndGameEffect;
import model.player.Player;

public class ImmunityToShamanicRitual extends OnEventEffect {

    /**
     * @implNote set true the attribute isImmuneToShaman
     * @param player
     */
    @Override
    public void applyOnShamanicRitual(Player player) {
        player.setImmuneToShaman(true);
    }
}
