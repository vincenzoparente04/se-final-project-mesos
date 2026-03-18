package model.cards.buildingCards;

import model.cards.Card;
import model.effects.BuildingEffect;


public class BuildingCard extends Card {
    private final int foodCost;
    private final int endGamePoints;
    private final Era era;
    private final BuildingEffect effect;


    // getters ---------------------------------------------------------------------------------------------------------
    public int getFoodCost(){ return foodCost; }
    public int getEndGamePoints(){ return endGamePoints; }
    @Override
    public Era getEra() {return era;}
    public BuildingEffect getEffect(){return effect;}
}

