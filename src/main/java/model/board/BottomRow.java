package model.board;


import model.cards.buildingCards.BuildingCard;
import model.cards.charachterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.TribeCard;
import model.deck.TribeDeck;

import java.util.List;

// Bottom row of the market, where players can recruit cards from
// the TribeCard are discarded at the end of the round
// the BuildingCard remain
// the EventCard present here are resolved at the end of the round
public class BottomRow {
    private List<TribeCard> tribeCards;
    private List<BuildingCard> buildingCards;

    // TODO: sono per popolare la row, da implementare
    public void addTribeCard(TribeCard card)
    public void addBuildingCard(BuildingCard card)
    public void populate(TribeDeck tribeDeck, int playerCount)
    {}

    public CharacterCard findCardById(int cardId)
    // scorre la lista interna e restituisce la carta con quell'id

    public boolean containsCard(int cardId)
    // controlla se la carta è presente

    public void removeCard(int cardId)
    // rimuove la carta dalla lista interna

    public boolean hasAvailableCards()
    // restituisce true se la lista non è vuota

    // TODO: non so come cazzo possiamo fa a filtrare gli eventi per tipo
    public List<EventCard> getSortedEvents() {
        // raccoglie tutti gli EventCard presenti nella riga

        // ordina: Sustenance sempre ultima
        // a parità di tipo → ordine per Era (I prima di II, II prima di III)

        return events;
    }

    public void discardTribeCards(){
        // tribeCards.clear()

    }

    public void receiveTribeCards(List<TribeCard> cards)
    // tribeCards.addAll(cards)

    // CODICE VECCHIO --------------------------------------------------------------------------------------------------

    public List<Card> getAllVisibleCards()
    public List<TribeCard> getTribeCards()
    public List<BuildingCard> getBuildingCards()
    public boolean isEmpty()
}