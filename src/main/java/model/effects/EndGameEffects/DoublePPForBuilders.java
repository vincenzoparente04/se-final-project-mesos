package model.effects.EndGameEffects;

import model.player.Player;

public class DoublePPForBuilders extends EndGameEffect{
    // add again builderPoints, no x2

    /**
     * @implNote Add prestige points for builders a second time if the player has the building with this effect
     * @param player
     */
    @Override
    public void applyEffect(Player player) {
        player.addPrestigePoints(player.getTribe().calculateBuildingPrintedPoints());
    }
}
