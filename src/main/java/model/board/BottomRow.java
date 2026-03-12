package model.board;


import model.cards.BuildingCard;
import model.cards.TribeCard;

// Bottom row of the market, where players can recruit cards from
// the TribeCard are discarded at the end of the round
// the BuildingCard remain
// the EventCard present here are resolved at the end of the round
public class BottomRow {
    private final List<TribeCard> tribeCards;
    private final List<BuildingCard> buildingCards;

    public void addTribeCard(TribeCard card)
    public void addBuildingCard(BuildingCard card)



    // gets or returns only the EventCard present — needed by the Controller to resolve them
    public List<EventCard> getEvents()

    // removes all the TribeCard (discard at the end of the round)
    // the BuildingCard remain in the BottomRow
    // REFACTOR: aggiungi metodo per scartare gli edifici se cambia l'era
    public List<TribeCard> discardTribeCards()

    // removes a specific card when a player takes it
    public void takeCard(Card card)

    public List<Card> getAllVisibleCards()
    public List<TribeCard> getTribeCards()
    public List<BuildingCard> getBuildingCards()
    public boolean isEmpty()
}