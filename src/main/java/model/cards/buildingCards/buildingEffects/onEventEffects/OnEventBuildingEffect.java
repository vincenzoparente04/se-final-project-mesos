package model.cards.buildingCards.buildingEffects.onEventEffects;

import model.cards.buildingCards.buildingEffects.BuildingEffect;
import model.player.Player;

public class OnEventBuildingEffect implements BuildingEffect {

    @Override
    public void registerSelf(Player player) {
        player.getTribe().registerOnEventEffect(this);
    }

    public int applyOnSustenance(Player player) { return 0; };
    public void applyOnShamanicRitual(Player player){};
    public void applyOnCavePaintings(Player player){};
    public void applyOnHunt(Player player){};
}
