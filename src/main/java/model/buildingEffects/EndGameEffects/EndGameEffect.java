package model.buildingEffects.EndGameEffects;

import model.buildingEffects.BuildingEffect;
import model.player.Player;

public class EndGameEffect implements BuildingEffect {

    @Override
    public void registerSelf(Player player) {
    }

    /**
     * @implNote generic applyEffect method to be overridden by sub classes
     * @param player
     */
    public void applyEffect(Player player) {};
}
