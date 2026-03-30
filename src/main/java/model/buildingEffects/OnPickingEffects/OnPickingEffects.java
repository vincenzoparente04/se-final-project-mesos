package model.buildingEffects.OnPickingEffects;

import model.buildingEffects.BuildingEffect;
import model.player.Player;

import java.util.function.Consumer;

public class OnPickingEffects implements BuildingEffect {
    private final Consumer<Player> flagSetter;

    public OnPickingEffects(Consumer<Player> flagSetter) {
        this.flagSetter = flagSetter;
    }

    @Override
    public void registerSelf(Player player) {
        flagSetter.accept(player);
    }
}

    /*
    Nella factory passo come BuildingEffect della BuildingCard:

    new OnPickingEffects(p -> p.setHasShamanicImmunity(true))
    new OnPickingEffects(p -> p.setHasShamanicBonusIcons(true))
    new OnPickingEffects(p -> p.setHasShamanicDoublePrestige(true))
    new OnPickingEffects(p -> p.setExtraDraw(true))
    new OnPickingEffects(p -> p.setExtraFoodOnTotemReturn(true))
     */