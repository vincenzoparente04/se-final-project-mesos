package model.board;

// Describes the effect of an offer tile
public class OfferTileAction {
    private final int topRowCards;     // number of cards to pick from the top row
    private final int bottomRowCards;  // number of cards to pick from the bottom row
    // REFACOTR: BASTA UN SONO ATTRIBUTO PER LA CASELLA CON CIBO
    // food to take: 3 for the first tile when playing in 5, 0 in all the other cases
    private final int foodAmount;

    public int getBottomRowCards() {
        return bottomRowCards;
    }

    public int getTopRowCards() {
        return topRowCards;
    }
}