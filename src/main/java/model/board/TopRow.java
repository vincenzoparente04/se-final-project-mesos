package model.board;
import java.util.List;

// Top row of the market, where players can recruit cards from
// building cards stay in the top row until taken, while tribe cards are removed at the end of the round and moved to the bottom row
public class TopRow {
    private final List<TribeCard> tribeCards;
    private final List<BuildingCard> buildingCards;

    // picks from the deck and fills the top row at the beginning of the round
    public void addTribeCard(TribeCard card)
    public void addBuildingCard(BuildingCard card)

    // removes and returns the TribeCard that need to go down to the BottomRow
    // the BuildingCard remain in the TopRow
    // REFACTOR: BISOGNA FARE LO STESSO PER I BUILDINGS QUANDO C'E' UNA NUOVA ERA
    public List<TribeCard> extractTribeCardsForBottomRow()

    // removes a specific card when a player takes it
    public void takeCard(Card card)

    // all visible cards in the top row, listed left to right (building cards are always on the right) for the view
    public List<Card> getAllVisibleCards()

    public List<TribeCard> getTribeCards()
    public List<BuildingCard> getBuildingCards()
    public boolean isEmpty()
}