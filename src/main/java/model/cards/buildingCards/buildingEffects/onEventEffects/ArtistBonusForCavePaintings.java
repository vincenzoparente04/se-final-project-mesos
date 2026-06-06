package model.cards.buildingCards.buildingEffects.onEventEffects;

import model.player.Player;

public class ArtistBonusForCavePaintings extends OnEventBuildingEffect {
    private final int foodMultiplier;

    public ArtistBonusForCavePaintings(int foodMultiplier){
        this.foodMultiplier = foodMultiplier;
    }

    /**
     *  give a food bonus for each artist in player's tribe during cave painting event
     */
    @Override
    public void applyOnCavePaintings(Player player) {
        player.addFood(player.getTribe().getArtistCount() * foodMultiplier);
    }
}
