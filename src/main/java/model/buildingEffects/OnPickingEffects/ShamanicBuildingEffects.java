package model.buildingEffects.OnPickingEffects;

import model.buildingEffects.BuildingEffect;
import model.player.Player;

import java.util.function.Consumer;

public class ShamanicBuildingEffects implements BuildingEffect {
    private final Consumer<Player> flagSetter;

    public ShamanicBuildingEffects(Consumer<Player> flagSetter) {
        this.flagSetter = flagSetter;
    }

    // TODO ricontrolla
    @Override
    public void registerSelf(Player player) {
        // si registra e chiama:
        flagSetter.accept(player); // equivale a dire: "quando chiamerai registerSelf, esegui player.setHasShamanicImmunity(true)".
    }

    /*
    Nella factory passo come BuildingEffect della BuildingCard:

    new ShamanicBuildingFlag(p -> p.setHasShamanicImmunity(true))
    new ShamanicBuildingFlag(p -> p.setHasShamanicBonusIcons(true))
    new ShamanicBuildingFlag(p -> p.setHasShamanicDoublePrestige(true))
     */
}