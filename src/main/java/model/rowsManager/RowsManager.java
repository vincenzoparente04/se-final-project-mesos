package model.rowsManager;

import model.cards.Card;
import model.cards.TribeCard;
import model.cards.buildingCards.BuildingCard;
import model.enums.Era;
import model.factories.BuildingCardFactory;
import model.factories.TribeCardFactory;
import model.player.Player;
import model.rowsManager.deck.BuildingDeck;
import model.rowsManager.deck.TribeDeck;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

//TODO: da capire come viene costruito il rows manager
public class RowsManager {
    private final List<TribeCard> topRowTribe;
    private final List<TribeCard> bottomRowTribe;
    private final List<BuildingCard> topRowBuilding;
    private final List<BuildingCard> bottomRowBuilding;

    private final TribeDeck tribeDeck;
    private final BuildingDeck buildingDeckEraI;
    private final BuildingDeck buildingDeckEraII;
    private final BuildingDeck buildingDeckEraIII;

    private final EventResolver eventResolver;

    public RowsManager() {
        this.topRowTribe      = new ArrayList<>();
        this.bottomRowTribe   = new ArrayList<>();
        this.topRowBuilding   = new ArrayList<>();
        this.bottomRowBuilding = new ArrayList<>();

        this.tribeDeck        = new TribeDeck();
        this.buildingDeckEraI   = new BuildingDeck(Era.ERA_I);
        this.buildingDeckEraII  = new BuildingDeck(Era.ERA_II);
        this.buildingDeckEraIII = new BuildingDeck(Era.ERA_III);

        this.eventResolver = new EventResolver();
    }

    /**
     * @implNote Initializes all decks and populates the starting rows (Setup, steps 3–6).
     *
     * 1. Creates all cards via the factories
     * 2. Initializes each deck (filtering by playerCount, shuffling, selecting)
     * 3. Draws the bottom row: playerCount + 1 tribe cards
     * 4. Draws the top row:    playerCount + 4 tribe cards
     * 5. Places all Era I building cards face up in the top row
     *
     * @param playerCount number of players in the game (2–5)
     */
    public void setup(int playerCount) {
        // create cards from JSON with factories
        TribeCardFactory.TribeCardCollection tribeCards = TribeCardFactory.createAll();
        List<BuildingCard> allBuildingCards = BuildingCardFactory.createAll();

        // initialize decks --
        tribeDeck.initializeDeck(tribeCards.regularCards(), tribeCards.finalEvents(), playerCount);
        buildingDeckEraI.initializeDeck(allBuildingCards, playerCount);
        buildingDeckEraII.initializeDeck(allBuildingCards, playerCount);
        buildingDeckEraIII.initializeDeck(allBuildingCards, playerCount);

        // populate starting rows --
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
        eventResolver.sortEvents(getAllTribeCardsOnBoard());
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
        topRowTribe.clear();
        topRowTribe.addAll(tribeDeck.drawMultiple(playerCount + 4));
    }

    /**
     * 1. discard all building cards from bottom row
     * 2. moves all building cards from top row to bottom row
     * 3. restores top row building cards, drawing from next era building deck
     */
    public void changeEra() {
        bottomRowBuilding.clear();
        bottomRowBuilding.addAll(topRowBuilding);
        topRowBuilding.clear();
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
    public List<TribeCard> getAllTribeCardsOnBoard() {
        List<TribeCard> allCards = new ArrayList<>(bottomRowTribe);
        allCards.addAll(topRowTribe);
        return allCards;
    }

    public List<Card> getAllCardsOnBoard() {
        List<Card> allCards = new ArrayList<>(bottomRowTribe);
        allCards.addAll(topRowTribe);
        allCards.addAll(bottomRowBuilding);
        allCards.addAll(topRowBuilding);
        return allCards;
    }

    public TribeDeck getTribeDeck() {
        return tribeDeck;
    }
}
