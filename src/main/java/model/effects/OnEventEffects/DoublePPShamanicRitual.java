package model.effects.OnEventEffects;

import model.player.Player;

public class DoublePPShamanicRitual extends OnEventEffect {

    /**
     * @implNote set true the attribute hasDoublePointsForShaman
     * @param player
     */
    @Override
    public void applyOnShamanicRitual(Player player) {
        player.setHasDoublePointsForShaman(true);
    }
}
