package model.effects.EndGameEffects;

import model.effects.BuildingEffect;
import model.player.Player;

public class EndGameEffect implements BuildingEffect {

    @Override
    public void registerSelf(Player player) {
    }

    public void applyEffect(Player player){};
    public void applyEffect(Player player, Character characterCard){};
}
