package model.effects.OnEventEffects;

import model.player.Player;

public class HunterBonusForHuntEvent extends OnEventEffect{
    /**
     * @implNote Add food and prestige points depending on the number of hunters in the tribe of the owner of the building card
     * @param player
     */
    @Override
    public void applyOnHunt(Player player) {
        player.addFood(player.getTribe().getHunterCount());
        player.addPrestigePoints(player.getTribe().getHunterCount());
    }
}
