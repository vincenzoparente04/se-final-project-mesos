package model.rowsManager.deck;

import model.cards.buildingCards.BuildingCard;
import model.enums.Era;

import java.util.List;

public class BuildingDeck {
    private Era era;
    private List<BuildingCard> cards;

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    //setup
    public void initializeDeck(int playerCount){
    }
    public BuildingDeck(Era era){}
    public void addCard(BuildingCard card)

    //access
    // used at the beginning of each Era to populate the top row with the Building cards of the current Era
    public List<BuildingCard> drawAll() {
        return cards;
    }

    public Era getEra(){return era;}
}
