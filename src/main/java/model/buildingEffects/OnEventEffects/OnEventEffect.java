package model.buildingEffects.OnEventEffects;

import model.buildingEffects.BuildingEffect;
import model.player.Player;

public class OnEventEffect implements BuildingEffect {

    @Override
    public void registerSelf(Player player) {

    }

    public int applyOnSustenance(Player player) { return 0; };
    public void applyOnShamanicRitual(Player player){};
    public void applyOnCavePaintings(Player player){};
    public void applyOnHunt(Player player){};
}
