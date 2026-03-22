package model.buildingEffects.OnEventEffects;

import model.player.Player;
import model.player.Tribe;

import java.util.function.ToIntFunction;

public class SustenanceDiscountEffect extends OnEventBuildingEffect {
    private final int discountPerCharacter;
    private final ToIntFunction<Tribe> correctGetter;

    public  SustenanceDiscountEffect(int discountPerCharacter, ToIntFunction<Tribe> correctGetter) {
        this.discountPerCharacter = discountPerCharacter;
        this.correctGetter = correctGetter;
    }


    /**
     * @implNote return the food discount for every correct character card. The discount is handled by the event card
     * @param player
     * @return int
     */
    @Override
    public int applyOnSustenance(Player player) {
        return correctGetter.applyAsInt(player.getTribe()) * discountPerCharacter;
    }
}
