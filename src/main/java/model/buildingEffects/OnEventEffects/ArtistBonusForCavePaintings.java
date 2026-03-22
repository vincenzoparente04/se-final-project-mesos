package model.buildingEffects.OnEventEffects;

import model.player.Player;

public class ArtistBonusForCavePaintings extends OnEventEffect{
    private final int foodMultiplier;

    public ArtistBonusForCavePaintings(int foodMultiplier){
        this.foodMultiplier = foodMultiplier;
    }

    /**
     * @implNote give a food bonus for each artist in player's tribe during cave painting event
     * @param player
     */
    @Override
    public void applyOnCavePaintings(Player player) {
        player.addFood(player.getTribe().getArtistCount() * foodMultiplier);
    }
}
