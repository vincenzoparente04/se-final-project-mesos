package model.cards.buildingCards;

import model.GameModel;
import model.cards.Card;
import model.effects.BuildingEffect;
import model.enums.Era;
import model.player.Player;


public class BuildingCard extends Card {
    private final int foodCost;
    private final int endGamePoints;
    private final BuildingEffect effect;

    public BuildingCard(int id, Era era, int playercount, int foodCost, int endGamePoints, BuildingEffect effect, BuildingEffect effect1) {
        super(id, era, playercount);
        this.foodCost = foodCost;
        this.endGamePoints = endGamePoints;
        this.effect = effect;
    }

    // getters
    public int getFoodCost(){ return foodCost; }
    public int getEndGamePoints(){ return endGamePoints; }
    public BuildingEffect getEffect(){return effect;}

    // override della register to tribe
    @Override
    public void registerToTribe(Player player, GameModel model) {
        player.getTribe().addBuilding(this);
    }

    public int getDiscountedCost(Player player) {
        int buildersDiscount = player.getTribe().getTotalBuilderDiscount();
        return Math.max(0, this.foodCost - buildersDiscount);
    }

    // ritorna true se il palyer ha abbastanza cibo al netto dello sconto applicato dai builder
    @Override
    public boolean canBeAcquiredBy(Player player, GameModel model) {
        return player.getFood() >= getDiscountedCost(player);
    }

    // rimuove il cibo si aggiunge
    @Override
    public void acquiredBy(Player player, GameModel model) {
        player.removeFood(getDiscountedCost(player), 0);
        registerToTribe(player, model);
    }
}

