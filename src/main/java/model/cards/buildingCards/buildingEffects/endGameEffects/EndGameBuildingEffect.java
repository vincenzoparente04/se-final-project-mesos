package model.cards.buildingCards.buildingEffects.endGameEffects;

import model.cards.buildingCards.buildingEffects.BuildingEffect;
import model.player.Player;
import model.player.Tribe;

import java.util.function.ToIntFunction;

public class EndGameBuildingEffect implements BuildingEffect {
    private final int multiplier;
    private final ToIntFunction<Tribe> getter;

    /**
     *  The constructor receives a multiplier for each character and the correct getter to use to count the number
     * of characters
     */
    public EndGameBuildingEffect(int multiplier, ToIntFunction<Tribe> getter) {
        this.multiplier = multiplier;
        this.getter = getter;
    }

    @Override
    public void registerSelf(Player player) {
        player.getTribe().registerEndGameEffect(this);
    }


    /**
     *  Add to the player the number of character times the multiplier
     */
    public void applyEffect(Player player) {
        player.addPrestigePoints(multiplier * getter.applyAsInt(player.getTribe()));
    }

    /* Nella CardFactory:

        // "3 PP per ogni Hunter"
        BuildingCard hunterBuilding = new BuildingCard(
        foodCost, era, printedPP,
        new EndGameBuildingEffect(3, Tribe::getHunterCount)
        );

        // "2 PP per ogni Artist"
        BuildingCard artistBuilding = new BuildingCard(
        foodCost, era, printedPP,
        new EndGameBuildingEffect(2, Tribe::getArtistCount)
        );

        // 6 PP per ogni set completo
        BuildingCard inventorBuilding = new BuildingCard(
        foodCost, era, printedPP,
        new EndGameBuildingEffect(6, Tribe::countCompleteSets)
        );

        // raddoppia PP dei builders
        BuildingCard inventorBuilding = new BuildingCard(
        foodCost, era, printedPP,
        new EndGameBuildingEffect(1, Tribe::calculateBuildersEndGamePoints)
        );

        // 25 PP flat
        BuildingCard inventorBuilding = new BuildingCard(
        foodCost, era, printedPP,
        new EndGameBuildingEffect(25, tribe -> 1)
        );
     */

}