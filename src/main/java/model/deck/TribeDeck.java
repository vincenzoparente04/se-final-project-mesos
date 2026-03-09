package model.deck;

import model.cards.TribeCard;
import model.enums.Era;

import java.util.Deque;
import java.util.List;

public class TribeDeck {
    // Deque: always draw from the top
    private Deque<TribeCard> cards;

    // keep track of the current Era based on the last card drawn
    private Era currentEra;

    // -- setup --
    public void initializeDeck(List<TribeCard> allCards, int playerCount)

    // used during setup to add cards to the deck
    public void addCard(TribeCard card)


    // -- draw --
    // ###servono entrambi sia draw che drawMultiple
    // draws the card from the top. Returns null if the deck is empty.
    public TribeCard draw()

    // draws multiple cards in sequence — used to populate the rows
    public List<TribeCard> drawMultiple(int count)


    // -- state --
    public boolean isEmpty()
    public int size()

    // REFACTOR: BISOGNA CAMBIARE O DRAWMULTIPLE O ISNEWERAREVEALED PERCHE' BISOGNA CONTROLLARE OGNI CARTA CHE VIENE MESSA SUL TABELLONE
    // PER CAPIRE SE SIAMO IN UNA NUOVA ERA.
    // INOLTRE BISOGNERA' IMPLEMENTARE UN METODO CHE QUAND CAMBIA ERA SPOSTA GLI EDIFICI NELLA FILA INFERIORE

    // returns true if the last drawn card belongs to a different Era than the currentEra,
    // the Controller checks this after every draw to trigger the start of a new Era
    public boolean isNewEraRevealed()

    public Era getCurrentEra()
}
