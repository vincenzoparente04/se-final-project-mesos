package model.buildingEffects.EndGameEffects;

import model.player.Player;

public class BonusForSet extends EndGameEffect {
        private final int prestigePointsPerSet;

        public BonusForSet(int prestigePointsPerSet) {
                this.prestigePointsPerSet = prestigePointsPerSet;
        }

    /**
     * @implNote Return a number of prestige points (passed through the constructor) for each complete set in player's tribe
     * @param player
     */
    @Override
        public void applyEffect(Player player) {
            player.addPrestigePoints(player.getTribe().countCompleteSets() * prestigePointsPerSet);
        }
}

