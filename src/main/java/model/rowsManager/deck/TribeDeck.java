package model.rowsManager.deck;

import model.cards.TribeCard;
import model.enums.Era;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class TribeDeck {
    private Deque<TribeCard> cards;
    private Era currentEra;

    // -- setup --
    public void initializeDeck(int playerCount) {}
    public void addCard(TribeCard card) {}


    /**
     * Draws a card from the top of the deck and updates the current Era based on the drawn card.
     * @return the drawn TribeCard
     */
    public TribeCard draw(){
        if (cards.isEmpty()) return null;
        TribeCard drawn = cards.pop();
        currentEra = drawn.getEra();
        return drawn;
    }

    /**
     * Draws multiple cards from the deck.
     * @param count
     * @return a list of drawn TribeCards
     */
    public List<TribeCard> drawMultiple(int count){
        List<TribeCard> drawn = new ArrayList<>();
        for (int i = 0; i<count; i++){
            drawn.add(draw());
        }
        return drawn;
    }


    // -- state --
    public boolean isEmpty() {
        return this.size() == 0;
    }

    public int size() {
        return cards.size();
    }

    //TODO: da inizializzare a 1 la current era
    public Era getCurrentEra(){
        return currentEra;
    }
}
