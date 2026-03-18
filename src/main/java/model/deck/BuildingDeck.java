package model.deck;

import model.cards.buildingCards.BuildingCard;

import java.util.List;

public class BuildingDeck {
    private int era;
    private List<BuildingCard> cards;

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    //setup
    public void initializeDeck(int playerCount){
    }
    public BuildingDeck(int era){}
    public void addCard(BuildingCard card)

    //access
    // used at the beginning of each Era to populate the top row with the Building cards of the current Era
    public List<BuildingCard> drawAll() {
        return cards;
    }

    public int getEra(){}
}
