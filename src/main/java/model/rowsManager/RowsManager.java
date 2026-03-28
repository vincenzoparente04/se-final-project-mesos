package model.rowsManager;

import model.cards.Card;
import model.cards.TribeCard;
import model.cards.buildingCards.BuildingCard;
import model.player.Player;
import model.rowsManager.deck.BuildingDeck;
import model.rowsManager.deck.TribeDeck;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

//TODO: da capire come viene costruito il rows manager
public class RowsManager {
    private List<TribeCard> topRowTribe;
    private List<TribeCard> bottomRowTribe;
    private List<BuildingCard> topRowBuilding;
    private List<BuildingCard> bottomRowBuilding;

    private TribeDeck tribeDeck;
    private BuildingDeck buildingDeckEraI;
    private BuildingDeck buildingDeckEraII;
    private BuildingDeck buildingDeckEraIII;

    private EventResolver eventResolver;

    public RowsManager() {
        this.eventResolver = new EventResolver();
    }

    public void setup(int playerCount) {
        tribeDeck.initializeDeck(playerCount);
        buildingDeckEraI.initializeDeck(playerCount);
        buildingDeckEraII.initializeDeck(playerCount);
        buildingDeckEraIII.initializeDeck(playerCount);
        bottomRowTribe.addAll(tribeDeck.drawMultiple(playerCount + 1));
        topRowTribe.addAll(tribeDeck.drawMultiple(playerCount + 4));
        topRowBuilding.addAll(buildingDeckEraI.drawAll());
    }

    /**
     * @implNote this method is responsible for resolving the events present in the bottom row at the end of the round, it is called by the EndOfRoundPhaseHandler
     * at first it collects all the EventCard in the bottom row. The EventResolver sorts events by type and era,
     * then it calls the resolve method of each EventCard, passing the list of players as parameter, so that the EventCard can apply its effect on the players.
     * @param players
     */
    public void resolveEvents(List<Player> players){
        eventResolver.sortEvents(bottomRowTribe);
        eventResolver.resolve(players);
    }

    /**
     * Solves all the events on the board. Called only by EndOfGamePhase
     * @implNote EventResolver sorts events by era and type, moving sustenance event at the end of the list from all cards present on the board. Then resolves all the events <br>
     * <b>NOTE: </b> to create the list of all cards present on the board it puts bottomRow cards first because maybe there could be some ERA_II card
     * @param players
     */
    public void resolveAllEvents(List<Player> players){
        eventResolver.sortEvents(getAllCardsOnBoard());
        eventResolver.resolve(players);
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

    /**
     * check if the card passed as argument is present in the top row
     * @param cardId
     */
    public boolean topRowContainsCard(int cardId) {
        return Stream.of(topRowTribe, topRowBuilding)
                .flatMap(List::stream)
                .anyMatch(c -> c.getId() == cardId);
    }

    /**
     * check if the card passed as argument is present in the bottom row
     * @param cardId
     */
    public boolean bottomRowContainsCard(int cardId) {
        return Stream.of(bottomRowTribe, bottomRowBuilding)
                .flatMap(List::stream)
                .anyMatch(c -> c.getId() == cardId);
    }

    /**
     * finds a card by its id between all 4 list (top row tribe, bottom row tribe, top row building, bottom row building)
     * @param cardId
     * @return the card object
     */
    public Card findCardById(int cardId) {
        return Stream.of(topRowTribe, bottomRowTribe, topRowBuilding, bottomRowBuilding)
                .flatMap(List::stream)
                .filter(c -> c.getId() == cardId)
                .findFirst()
                .orElse(null); // restituisce null se non trova nessuna carta con quell'ID
    }

    /**
     * @implNote search the card through all 4 lists and removes it
     * @param cardId
     */
    public void removeCard(int cardId) {
        Stream.of(topRowTribe, bottomRowTribe, topRowBuilding, bottomRowBuilding)
                .forEach(list -> list.removeIf(card -> card.getId() == cardId));
    }

    /**
     * @implNote merges top row and bottom row tribe cards. The returned list has bottom row cards first, then top row cards.
     * @return a list of all the tribe cards present on the board, both in the top and bottom row
     */
    public List<TribeCard> getAllCardsOnBoard() {
        List<TribeCard> allCards = new ArrayList<>(bottomRowTribe);
        allCards.addAll(topRowTribe);
        return allCards;
    }

    public TribeDeck getTribeDeck() {
        return tribeDeck;
    }
}
