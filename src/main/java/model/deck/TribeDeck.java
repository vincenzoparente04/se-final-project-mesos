package model.deck;

import model.cards.TribeCard;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class TribeDeck {
    // Deque: always draw from the top
    private Deque<TribeCard> cards;

    // keep track of the current Era based on the last card drawn
    private int currentEra;

    // -- setup --
    public void initializeDeck(List<TribeCard> allCards, int playerCount)

    // used during setup to add cards to the deck
    public void addCard(TribeCard card)


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

    // REFACTOR: BISOGNA CAMBIARE O DRAWMULTIPLE O ISNEWERAREVEALED PERCHE' BISOGNA CONTROLLARE OGNI CARTA CHE VIENE MESSA SUL TABELLONE
    // PER CAPIRE SE SIAMO IN UNA NUOVA ERA.
    // INOLTRE BISOGNERA' IMPLEMENTARE UN METODO CHE QUAND CAMBIA ERA SPOSTA GLI EDIFICI NELLA FILA INFERIORE

    // returns true if the last drawn card belongs to a different Era than the currentEra,
    // the Controller checks this after every draw to trigger the start of a new Era
    public boolean isNewEraRevealed(){}

    //TODO: da inizializzare a 1 la current era
    public int getCurrentEra(){
        return currentEra;
    }
}
