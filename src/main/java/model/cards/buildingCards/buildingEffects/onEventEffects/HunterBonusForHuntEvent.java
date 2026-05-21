package model.cards.buildingCards.buildingEffects.onEventEffects;

import model.player.Player;

public class HunterBonusForHuntEvent extends OnEventBuildingEffect {
    private final int prestigeMultiplier;
    private final int foodMultiplier;

    public HunterBonusForHuntEvent(int prestigeMultiplier, int foodMultiplier){
        this.foodMultiplier = foodMultiplier;
        this.prestigeMultiplier = prestigeMultiplier;
    }

    /**
     * @implNote Add food and prestige points depending on the number of hunters in the tribe of the owner of the building
     * card during hunt event
     * @param player
     */
    @Override
    public void applyOnHunt(Player player) {
        player.addFood(player.getTribe().getHunterCount() * foodMultiplier);
        player.addPrestigePoints(player.getTribe().getHunterCount() * prestigeMultiplier);
    }
}
