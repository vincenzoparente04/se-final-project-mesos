package model.cards.buildingCards;

import model.GameModel;
import model.cards.Card;
import model.buildingEffects.BuildingEffect;
import model.enums.Era;
import model.player.Player;


public class BuildingCard extends Card {
    private final int foodCost;
    private final int endGamePoints;
    private final BuildingEffect effect;

    public BuildingCard(int id, Era era, int playercount, int foodCost, int endGamePoints, BuildingEffect effect, String imagePath, String backImagePath) {
        super(id, era, playercount, imagePath, backImagePath);
        this.foodCost = foodCost;
        this.endGamePoints = endGamePoints;
        this.effect = effect;
    }

    // getters
    public int getFoodCost(){ return foodCost; }
    public int getEndGamePoints(){ return endGamePoints; }
    public BuildingEffect getEffect(){return effect;}

    /**
     * @implNote Add the building card and the effect in the tribe of the player who picked the card.
     * @param player
     */
    @Override
    public void registerToTribe(Player player) {
        player.getTribe().addBuilding(this);
        this.effect.registerSelf(player);
    }

    /**
     * @implNote Calculate the discounted cost of the building card, applying any builder discount the
     * player may have. The cost cannot be negative.
     * @param player
     * @return the right amount to pay for the building card
     */
    public int getDiscountedCost(Player player) {
        int buildersDiscount = player.getTribe().getTotalBuilderDiscount();
        return Math.max(0, this.foodCost - buildersDiscount);
    }

    /**
     * @implNote Checks if the player has enough food to pay the building card given the builders discount.
     * @param player
     * @param model
     * @return
     */
    @Override
    public boolean canBeAcquiredBy(Player player, GameModel model) {
        return player.getFood() >= getDiscountedCost(player);
    }

    /**
     * @implNote Remove the food from the player, and add the card to the player's tribe.
     * @param player
     * @param model
     */
    @Override
    public void acquiredBy(Player player, GameModel model) {
        player.removeFood(getDiscountedCost(player), 0);
        registerToTribe(player);
    }
}

