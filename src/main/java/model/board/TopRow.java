package model.board;
import model.cards.BuildingCard;
import model.cards.CharacterCard;
import model.cards.TribeCard;
import model.deck.TribeDeck;

import java.util.List;

// Top row of the market, where players can recruit cards from
// building cards stay in the top row until taken, while tribe cards are removed at the end of the round and moved to the bottom row
public class TopRow {
    private List<TribeCard> tribeCards;
    private List<BuildingCard> buildingCards;

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


    public List<TribeCard> extractTribeCardsForBottomRow() {
        // salva riferimento alla lista corrente
        // resetta tribeCards a lista vuota
        // restituisce la lista salvata
    }

    public void restore(TribeDeck tribeDeck, int playerCount)
    // pesca playerCount+4 carte e le aggiunge a tribeCards

    // CODICE VECCHIO -----------------------------------------------------------------------------------------------------
    // all visible cards in the top row, listed left to right (building cards are always on the right) for the view
    public List<Card> getAllVisibleCards()

    public List<TribeCard> getTribeCards()
    public List<BuildingCard> getBuildingCards()
    public boolean isEmpty()
}