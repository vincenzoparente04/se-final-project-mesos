package model.effects.OnCharacterAcquiredEffects;

import model.cards.charachterCards.CharacterCard;
import model.effects.BuildingEffect;
import model.player.Player;

public abstract class OnCharacterAcquiredEffect implements BuildingEffect {

    @Override
    public void registerSelf(Player player) {
        // si registra nella lista OnAcquire
    }

    public abstract void applyEffect(Player player);
}
