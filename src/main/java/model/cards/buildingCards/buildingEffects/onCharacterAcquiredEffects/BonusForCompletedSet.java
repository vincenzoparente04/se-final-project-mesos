package model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects;

import model.player.Player;

public class BonusForCompletedSet extends OnAcquireBuildingEffect {
    private int alreadyCompletedSets;

    /**
     * @implNote override registerSelf to save the already completed sets in the moment of acquiring
     */
    @Override
    public void registerSelf(Player player) {
        this.alreadyCompletedSets = player.getTribe().countCompleteSets();
        player.getTribe().registerOnAcquireEffect(this);
    }

    /**
     * @implNote After the drawing this method checks if the number of complete sets has increased from previous round
     */
    @Override
    public void applyEffect(Player player) {
        int currentSets = player.getTribe().countCompleteSets();
        if (currentSets > alreadyCompletedSets) {
            player.addFood(5);
            alreadyCompletedSets = currentSets;
        }
    }
}
