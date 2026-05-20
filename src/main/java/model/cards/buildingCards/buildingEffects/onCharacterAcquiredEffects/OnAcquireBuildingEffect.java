package model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects;

import model.cards.buildingCards.buildingEffects.BuildingEffect;
import model.player.Player;

public abstract class OnAcquireBuildingEffect implements BuildingEffect {

    @Override
    public void registerSelf(Player player) {
        player.getTribe().registerOnAcquireEffect(this);
    }

    public abstract void applyEffect(Player player);
}
