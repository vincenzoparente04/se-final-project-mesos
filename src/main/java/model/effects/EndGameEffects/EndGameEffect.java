package model.effects.EndGameEffects;

import model.effects.BuildingEffect;
import model.player.Player;

import java.util.List;

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
