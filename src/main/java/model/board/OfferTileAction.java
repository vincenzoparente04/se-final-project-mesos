package model.board;

// Describes the effect of an offer tile
public class OfferTileAction {
    private final int topRowCards;     // number of cards to pick from the top row
    private final int bottomRowCards;  // number of cards to pick from the bottom row
    // REFACOTR: BASTA UN SONO ATTRIBUTO PER LA CASELLA CON CIBO
    private final boolean foodOnly;    // true only for the A tile
    private final int foodAmount;      // food to take if foodOnly == true
}