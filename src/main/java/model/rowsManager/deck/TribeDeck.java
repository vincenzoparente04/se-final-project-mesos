package model.rowsManager.deck;

import model.cards.TribeCard;
import model.enums.Era;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class TribeDeck {
    // Deque: always draw from the top
    private Deque<TribeCard> cards;

    // keep track of the current Era based on the last card drawn
    private Era currentEra;

    // -- setup --
    public void initializeDeck(int playerCount) {}

    // used during setup to add cards to the deck
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
    public boolean isEmpty()
    public int size()


    //TODO: da inizializzare a 1 la current era
    public Era getCurrentEra(){
        return currentEra;
    }
}
