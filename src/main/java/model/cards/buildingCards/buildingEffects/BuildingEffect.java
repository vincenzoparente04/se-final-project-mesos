package model.cards.buildingCards.buildingEffects;

import model.player.Player;

public interface BuildingEffect {

    public void registerSelf(Player player);
}