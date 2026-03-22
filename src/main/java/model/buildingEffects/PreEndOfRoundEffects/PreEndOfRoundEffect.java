package model.buildingEffects.PreEndOfRoundEffects;

import model.buildingEffects.BuildingEffect;
import model.player.Player;

public abstract class PreEndOfRoundEffect implements BuildingEffect {

    @Override
    public void registerSelf(Player player) {
        player.registerPreEndOfRoundEffect(this);
    }

    public abstract boolean needsPlayerInput();
    public abstract void applyEffect(Player player);
}
