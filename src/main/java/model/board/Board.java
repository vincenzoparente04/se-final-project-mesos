package model.board;

// The main board class that contains all the components of the board and provides access to them for the Controller and the View.
// It is only responsible for holding the state of the board and providing access to its components, while the Controller is
// responsible for applying the game logic and mutating the state of the board accordingly; the View is responsible for
// displaying the current state of the board.

import model.cards.TribeCard;
import model.cards.charachterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.deck.BuildingDeck;
import model.deck.TribeDeck;
import model.player.Player;
import model.player.Totem;

import java.util.ArrayList;
import java.util.List;

import static model.enums.TotemLocation.TURN_ORDER_TILE;

public class Board implements CardVisitor {
    private final OfferTrack offerTrack;
    private final TurnOrderTile turnOrderTile;
    private TopRow topRow;
    private BottomRow bottomRow;
    private List<EventCard> eventsToResolve;
    private List<SustenanceEventCard> sustenanceToResolve;

    private TribeDeck tribeDeck;
    private BuildingDeck buildingDeckEraI;
    private BuildingDeck buildingDeckEraII;
    private BuildingDeck buildingDeckEraIII;

    public OfferTrack getOfferTrack() {
        return offerTrack;
    }

    public TribeDeck getTribeDeck() {
        return tribeDeck;
    }

    public TurnOrderTile getTurnOrderTile() {
        return turnOrderTile;
    }

    public void setup(int playerCount){
        tribeDeck.initializeDeck(playerCount);
        buildingDeckEraI.initializeDeck(playerCount);
        buildingDeckEraII.initializeDeck(playerCount);
        buildingDeckEraIII.initializeDeck(playerCount);
        turnOrderTile.setup(playerCount);
        offerTrack.setup(playerCount);
        topRow.restore(tribeDeck, playerCount);
        topRow.addBuildingCard(); // in loop o come cazzo ve pare
        bottomRow.populate(tribeDeck, playerCount);
    }

    // TODO non ha senso che stia qui
    /**
     * Randomizes the turn order by shuffling the list of players and placing their totems on the TurnOrderTile in the new order.
     * @param players
     */
    public void randomizeTurnOrder(List<Player> players){
        List<TurnOrderSlot> slots = turnOrderTile.getSlots();

        //shuffles the players list
        List<Player> randomized = new ArrayList<>(players);
        java.util.Collections.shuffle(randomized);

        //for each player in the shuffled list, places their totem on the corresponding slot on the TurnOrderTile and updates the totem's location
        for (int i =0; i < randomized.size(); i++) {
            Totem t = randomized.get(i).getTotem();

            slots.get(i).placeTotem(t);
            t.setLocation(TURN_ORDER_TILE);
        }
    }

    /**
     * Delegate the placeTotem implementation to the offerTrack
     * @param totem
     * @param offerTile
     * @throws Exception
     */
    public void placeTotem(Totem totem, OfferTile offerTile) throws Exception {
        offerTrack.placeTotem(totem, offerTile);
    }

    // helpers:
    private void populateBottomRow(TribeDeck tribeDeck, int playerCount){
        tribeDeck.draw(); // in loop
    }
    private void populateTopRow(TribeDeck tribeDeck, BuildingDeck buildingDeckI, int playerCount){
        tribeDeck.draw(); // in loop
        buildingDeckI.drawAll();
    }

    public CharacterCard findCardById(int cardId) {
        CharacterCard card = topRow.findCardById(cardId);
        if (card != null) return card;
        return bottomRow.findCardById(cardId);
    }

    public void removeCard(int cardId) {
        if (topRow.containsCard(cardId)) topRow.removeCard(cardId);
        else bottomRow.removeCard(cardId);
    }

    /**
     * @implNote this method is responsible for resolving the events present in the bottom row at the end of the round, it is called by the EndOfRoundPhaseHandler
     * at first it collects all the EventCard in the bottom row. BottomRow returns the events already sorted by type and era,
     * then it calls the resolve method of each EventCard, passing the list of players as parameter, so that the EventCard can apply its effect on the players.
     * @param players
     */
    public void resolveEvents(List<Player> players){
        List<EventCard> events = bottomRow.getSortedEvents();
        for (EventCard event : events) {
            event.resolve(players);
        }
    }


    /**
     * Solves all the events on the board. Called only by EndOfGamePhase
     * @implNote sort events by era and type, moving sustenance event at the end of the list from all cards present on the board. Then resolves all the events <br>
     * <b>NOTE: </b> to create the list of all cards present on the board it puts bottomRow cards first because maybe there could be some ERA_II card
     * @param players
     */
    public void resolveAllEvents(List<Player> players){
        List<TribeCard> allCards = new ArrayList<>(bottomRow.getTribeCards());
        allCards.addAll(topRow.getTribeCards());

        eventsToResolve = new ArrayList<EventCard>();
        sustenanceToResolve = new ArrayList<SustenanceEventCard>();
        for(TribeCard card : allCards){
            card.accept(this);
        }
        eventsToResolve.addAll(sustenanceToResolve);

        for (EventCard event : eventsToResolve) {
            event.resolve(players);
        }
    }

    @Override
    public void visit(CharacterCard card) {
        // do nothing, there are no character card in the bottom row
    }
    @Override
    public void visit(EventCard card) {
        eventsToResolve.add(card);
    }
    @Override
    public void visit(SustenanceEventCard card) {
        sustenanceToResolve.add(card);
    }


    public void endRound(int playerCount) {
        bottomRow.discardTribeCards();
        bottomRow.receiveTribeCards(topRow.extractTribeCardsForBottomRow());
        topRow.restore(tribeDeck, playerCount);
    }

    public void changeEra() {
        bottomRow.discardBuildingCards();
        bottomRow.receiveBuildingCards(topRow.extractBuildingCardsForBottomRow());
        if (buildingDeckEraII.isEmpty()) {
            topRow.restoreEra(buildingDeckEraIII);
        }else{
            topRow.restoreEra(buildingDeckEraII);
        }
    }

    public BottomRow getBottomRow() {
        return bottomRow;
    }

    public TopRow getTopRow() {
        return topRow;
    }
}