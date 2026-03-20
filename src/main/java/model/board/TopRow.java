package model.board;
import model.cards.buildingCards.BuildingCard;
import model.cards.charachterCards.CharacterCard;
import model.cards.TribeCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.deck.BuildingDeck;
import model.deck.TribeDeck;

import java.util.ArrayList;
import java.util.List;

// Top row of the market, where players can recruit cards from
// building cards stay in the top row until taken, while tribe cards are removed at the end of the round and moved to the bottom row
public class TopRow {
    private List<TribeCard> tribeCards;
    private List<BuildingCard> buildingCards;
    private List<EventCard> eventsToResolve;
    private List<SustenanceEventCard> sustenanceToResolve;

    // picks from the deck and fills the top row at the beginning of the round
    public void addTribeCard(TribeCard card)
    public void addBuildingCard(BuildingCard card)

    public CharacterCard findCardById(int cardId)
    // scorre la lista interna e restituisce la carta con quell'id

    public boolean containsCard(int cardId)
    // controlla se la carta è presente

    public void removeCard(int cardId)
    // rimuove la carta dalla lista interna

    public boolean hasAvailableCards()
    // restituisce true se la lista non è vuota

    /**
     * @implNote returns all tribe cards in the top row and removes them from the top row
     * @return all tribe cards in the top row
     */
    public List<TribeCard> extractTribeCardsForBottomRow() {
        List<TribeCard> cardsToMove = new ArrayList<>(tribeCards);
        tribeCards.clear();
        return cardsToMove;
    }

    /**
     * @implNote returns all building cards in the top row and removes them from the top row
     * @return all building cards in the top row
     */
    public List<BuildingCard> extractBuildingCardsForBottomRow() {
        List<BuildingCard> buildingCardsToMove = new ArrayList<>(buildingCards);
        buildingCards.clear();
        return  buildingCardsToMove;
    }


    /**
     * @implNote this method is responsible for restoring the top row drawing cards from tribe deck
     * @param tribeDeck
     * @param playerCount
     */
    public void restore(TribeDeck tribeDeck, int playerCount){
        // chiama drawMultiple su tribeDeck per riempire la lista dei tribeCards, in base al numero di giocatori
        // chiama drawAll su buildingDeckI per riempire la lista dei buildingCards
        int cardsToDraw = playerCount + 4;
        tribeCards.addAll(tribeDeck.drawMultiple(cardsToDraw));
    }

    /**
     * @implNote this method is responsible for restoring the top row at the beginning of each Era, it is called by the EndOfRoundPhaseHandler at the end of each round, after the end of round phase has been resolved<br>
     * @param buildingDeckEra
     */
    public void restoreEra(BuildingDeck buildingDeckEra){
        buildingCards.addAll(buildingDeckEra.drawAll());
    }



    // CODICE VECCHIO -----------------------------------------------------------------------------------------------------
    // all visible cards in the top row, listed left to right (building cards are always on the right) for the view
    public List<Card> getAllVisibleCards()

    public List<TribeCard> getTribeCards()
    public List<BuildingCard> getBuildingCards()
    public boolean isEmpty()
}