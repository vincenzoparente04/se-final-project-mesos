package model.cards.buildingCards;

import model.cards.Card;
import model.cards.buildingCards.buildingEffects.BuildingEffect;
import model.enums.Era;
import model.player.Player;
import model.rowsManager.CardVisitor;


public class BuildingCard extends Card {
    private final int foodCost;
    private final int endGamePoints;
    private final BuildingEffect effect;
    private final String effectId;          // used to specify the effect of the card in the cli


    public BuildingCard(int id, Era era, int playercount, int foodCost, int endGamePoints, BuildingEffect effect, String effectId, String imagePath, String backImagePath) {
        super(id, era, playercount, imagePath, backImagePath);
        this.foodCost = foodCost;
        this.endGamePoints = endGamePoints;
        this.effect = effect;
        this.effectId      = effectId;
    }

    // getters
    public int getFoodCost(){ return foodCost; }
    public int getEndGamePoints(){ return endGamePoints; }
    public BuildingEffect getEffect(){return effect;}
    public String getEffectId()      { return effectId; }

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

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}

