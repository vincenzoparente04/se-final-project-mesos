package model.board;

import model.cards.Card;
import model.cards.TribeCard;
import model.cards.buildingCards.BuildingCard;
import model.cards.charachterCards.BuilderCard;
import model.cards.charachterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.deck.BuildingDeck;
import model.deck.TribeDeck;
import model.enums.Era;
import model.enums.TotemLocation;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static model.enums.TotemLocation.TURN_ORDER_TILE;

public class Board implements CardVisitor {
    private final OfferTrack offerTrack;
    private final TurnOrderTile turnOrderTile;

    private List<TribeCard> topRowTribe;
    private List<TribeCard> bottomRowTribe;
    private List<BuildingCard> topRowBuilding;
    private List<BuildingCard> bottomRowBuilding;

    private List<EventCard> eventsToResolve;
    private List<SustenanceEventCard> sustenanceToResolve;

    private TribeDeck tribeDeck;
    private BuildingDeck buildingDeckEraI;
    private BuildingDeck buildingDeckEraII;
    private BuildingDeck buildingDeckEraIII;

    public Board() {  // TODO: check how we want to construct the board
        this.topRowTribe = new ArrayList<>();
        this.bottomRowTribe = new ArrayList<>();
        this.topRowBuilding = new ArrayList<>();
        this.bottomRowBuilding = new ArrayList<>();
        this.buildingDeckEraI = new BuildingDeck(Era.ERA_I);
        this.buildingDeckEraII = new BuildingDeck(Era.ERA_II);
        this.buildingDeckEraIII = new BuildingDeck(Era.ERA_III);
        this.tribeDeck = new TribeDeck();
        this.offerTrack = new OfferTrack();
        this.turnOrderTile = new TurnOrderTile();
    }

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
        bottomRowTribe.addAll(tribeDeck.drawMultiple(playerCount + 1));
        topRowTribe.addAll(tribeDeck.drawMultiple(playerCount + 4));
        topRowBuilding.addAll(buildingDeckEraI.drawAll());
    }

    //TODO probabilmente va spostata la logica del randomize in setUpPhase, che poi chiamerà semplicemente placeTotem
    /**
     * Randomizes the turn order by shuffling the list of players and placing their totems on the TurnOrderTile in the new order.
     * @param players
     */
    public void randomizeTurnOrder(List<Player> players){
        List<TurnOrderSlot> slots = turnOrderTile.getSlots();

        // 1. Mischia la lista dei giocatori per determinare l'ordine casuale iniziale
        List<Player> randomized = new ArrayList<>(players);
        java.util.Collections.shuffle(randomized);

        // 2. Posiziona i giocatori negli slot e assegna il cibo secondo il regolamento
        for (int i = 0; i < randomized.size(); i++) {
            Player p = randomized.get(i);
            slots.get(i).placeTotem(p);
            p.setLocation(TotemLocation.TURN_ORDER_TILE);
        }
    }

    /**
     * Delegate the placeTotem implementation to the offerTrack
     * @param player
     * @param offerTile
     * @throws Exception
     */
    public void placeTotem(Player player, OfferTile offerTile) throws Exception {
        offerTrack.placeTotem(player, offerTile);
    }

    // helpers:
    private void populateBottomRow(TribeDeck tribeDeck, int playerCount){
        tribeDeck.draw(); // in loop
    }
    private void populateTopRow(TribeDeck tribeDeck, BuildingDeck buildingDeckI, int playerCount){
        tribeDeck.draw(); // in loop
        buildingDeckI.drawAll();
    }


    /**
     * @implNote this method is responsible for resolving the events present in the bottom row at the end of the round, it is called by the EndOfRoundPhaseHandler
     * at first it collects all the EventCard in the bottom row. getSortedEvents() returns the events already sorted by type and era,
     * then it calls the resolve method of each EventCard, passing the list of players as parameter, so that the EventCard can apply its effect on the players.
     * @param players
     */
    public void resolveEvents(List<Player> players){
        getSortedEvents(bottomRowTribe);
        resolve(players);
    }

    /**
     * Solves all the events on the board. Called only by EndOfGamePhase
     * @implNote sort events by era and type, moving sustenance event at the end of the list from all cards present on the board. Then resolves all the events <br>
     * <b>NOTE: </b> to create the list of all cards present on the board it puts bottomRow cards first because maybe there could be some ERA_II card
     * @param players
     */
    public void resolveAllEvents(List<Player> players){
        getSortedEvents(getAllCardsOnBoard());
        resolve(players);
    }

    /**
     * resolve all events in eventsToResolve which is previously sorted by getSortedEvents() method, it is called by resolveEvents() and resolveAllEvents() methods
     * @param players
     */
    public void resolve(List<Player> players){
        for (EventCard event : eventsToResolve) {
            event.resolve(players);
        }
    }

    /**
     * @implNote collects all the events present in the bottom row (to be resolved) and it sorts them moving the sustenance event at the end of the list<br>
     * <p><b>Note, this method works only if it's assumed that the cards order in the bottom row is the unmuted from the original order in the top row
     * which has to be the same as picking order from the deck</b></p>
     * @return the list of events to resolve, sorted by type and era
     */
    public void getSortedEvents(List<TribeCard> tribeToSort) {
        eventsToResolve = new ArrayList<EventCard>();
        sustenanceToResolve = new ArrayList<SustenanceEventCard>();
        for(TribeCard card : tribeToSort){
            card.accept(this);
        }
        eventsToResolve.addAll(sustenanceToResolve);
    }

    @Override
    public void visit(CharacterCard card) {
        // do nothing, CharacterCard does not have any effect to resolve
    }
    @Override
    public void visit(EventCard card) {
        eventsToResolve.add(card);
    }
    @Override
    public void visit(SustenanceEventCard card) {
        sustenanceToResolve.add(card);
    }

    /**
     * 1. discard all tribe cards from bottom row
     * 2. moves all tribe cards from top row to bottom row
     * 3. restores top row tribe cards, drawing from tribe deck
     * @param playerCount
     */
    public void endRound(int playerCount) {
        bottomRowTribe.clear();
        bottomRowTribe.addAll(topRowTribe);
        int cardsToDraw = playerCount + 4;
        topRowTribe.addAll(tribeDeck.drawMultiple(cardsToDraw));
    }

    /**
     * 1. discard all building cards from bottom row
     * 2. moves all building cards from top row to bottom row
     * 3. restores top row building cards, drawing from next era building deck
     */
    public void changeEra() {
        bottomRowBuilding.clear();
        bottomRowBuilding.addAll(topRowBuilding);
        if (buildingDeckEraII.isEmpty()) {
            topRowBuilding.addAll(buildingDeckEraIII.drawAll());
        }else{
            topRowBuilding.addAll(buildingDeckEraII.drawAll());
        }
    }

    public List<TribeCard> getAllCardsOnBoard() {
        List<TribeCard> allCards = new ArrayList<>(bottomRowTribe);
        allCards.addAll(topRowTribe);
        return allCards;
    }

    public void getNextPlyerOnOfferTrack() {

    }

    public boolean topRowContainsCard(int cardId) {
        return Stream.of(topRowTribe, topRowBuilding)
                .flatMap(List::stream)
                .anyMatch(c -> c.getId() == cardId);
    }

    public boolean bottomRowContainsCard(int cardId) {
        return Stream.of(bottomRowTribe, bottomRowBuilding)
                .flatMap(List::stream)
                .anyMatch(c -> c.getId() == cardId);
    }

    public Card findCardById(int cardId) {
        return Stream.of(topRowTribe, bottomRowTribe, topRowBuilding, bottomRowBuilding)
                .flatMap(List::stream)
                .filter(c -> c.getId() == cardId)
                .findFirst()
                .orElse(null); // restituisce null se non trova nessuna carta con quell'ID
    }

    public void removeCard(int cardId) {
        Stream.of(topRowTribe, bottomRowTribe, topRowBuilding, bottomRowBuilding)
              .forEach(list -> list.removeIf(card -> card.getId() == cardId));
    }
}
