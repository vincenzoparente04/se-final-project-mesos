package model.board;


import model.cards.buildingCards.BuildingCard;
import model.cards.charachterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.TribeCard;
import model.cards.eventCards.SustenanceEventCard;
import model.deck.TribeDeck;
import model.player.Player;

import java.util.List;

// Bottom row of the market, where players can recruit cards from
// the TribeCard are discarded at the end of the round
// the BuildingCard remain
// the EventCard present here are resolved at the end of the round
public class BottomRow implements CardVisitor {
    private List<TribeCard> tribeCards;
    private List<BuildingCard> buildingCards;
    private List<EventCard> eventsToResolve;
    private List<SustenanceEventCard> sustenanceToResolve;

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

    /**
     * @implNote collects all the events present in the bottom row (to be resolved) and it sorts them moving the sustenance event at the end of the list<br>
     * <p><b>Note, this method works only if it's assumed that the cards order in the bottom row is the unmuted from the original order in the top row
     * which has to be the same as picking order from the deck</b></p>
     * @return the list of events to resolve, sorted by type and era
     */
    public List<EventCard> getSortedEvents() {
        // raccoglie tutti gli EventCard presenti nella riga

        // ordina: Sustenance sempre ultima
        // a parità di tipo → ordine per Era (I prima di II, II prima di III)

        for(TribeCard card : tribeCards){
            card.accept(this);
        }
        eventsToResolve.addAll(sustenanceToResolve);
        return eventsToResolve;
    }

    @Override
    public void visit(CharacterCard card) {
        // non fa niente, i CharacterCard non sono eventi
    }
    @Override
    public void visit(EventCard card) {
        // aggiunge l'EventCard alla lista degli eventi da risolvere
        eventsToResolve.add(card);
    }
    @Override
    public void visit(SustenanceEventCard card) {
        sustenanceToResolve.add(card);
    }


    public void discardTribeCards(){
        tribeCards.clear();
    }

    public void receiveTribeCards(List<TribeCard> cards) {
        tribeCards.addAll(cards);
    }
}