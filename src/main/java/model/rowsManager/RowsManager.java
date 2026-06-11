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
import shared.dto.event.EventResolutionDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Manages the four card rows that form the central board offer, together with
 * the underlying decks and event resolution logic.
 *
 * <p>The board is organised into two parallel offer tracks:
 * <ul>
 *   <li><b>Tribe track</b> — a top row and a bottom row of {@link TribeCard}s
 *       drawn from a single {@link TribeDeck}.</li>
 *   <li><b>Building track</b> — a top row and a bottom row of
 *       {@link BuildingCard}s. Cards progress across three era-specific
 *       {@link BuildingDeck}s: Era I fill the top row at setup, and each
 *       era transition promotes the current top row to the bottom and loads
 *       the next era deck into the top.</li>
 * </ul>
 *
 * <p>Event cards embedded in the tribe rows are resolved through a dedicated
 * {@link EventResolver}.
 *
 * <p>The typical lifecycle of this class across a game is:
 * <ol>
 *   <li>{@link #setup(int)} — called once during {@code SetupPhase} to
 *       initialise decks and populate the starting rows.</li>
 *   <li>{@link #resolveEvents(List)} — called each round by
 *       {@code EndOfRoundPhase} to resolve events in the bottom tribe row.</li>
 *   <li>{@link #endRound(int)} — called each round to rotate tribe rows.</li>
 *   <li>{@link #changeEra()} — called when the era advances to rotate
 *       building rows and load the next era deck.</li>
 *   <li>{@link #resolveAllEvents(List)} — called once by
 *       {@code EndOfGamePhase} to resolve all remaining events on the board.</li>
 * </ol>
 */public class RowsManager {
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
        this.topRowTribe = new ArrayList<>();
        this.bottomRowTribe = new ArrayList<>();
        this.topRowBuilding = new ArrayList<>();
        this.bottomRowBuilding = new ArrayList<>();

        this.tribeDeck = new TribeDeck();
        this.buildingDeckEraI = new BuildingDeck(Era.ERA_I);
        this.buildingDeckEraII = new BuildingDeck(Era.ERA_II);
        this.buildingDeckEraIII = new BuildingDeck(Era.ERA_III);

        this.eventResolver = new EventResolver();
    }

    /**
     * Initializes all decks and populates the starting rows (Setup, steps 3–6).
     *
     * <p>1. Creates all cards via the factories;
     * <p>2. Initializes each deck (filtering by playerCount, shuffling, selecting);
     * <p>3. Draws the bottom row: playerCount + 1 tribe cards;
     * <p>4. Draws the top row:    playerCount + 4 tribe cards;
     * <p>5. Places all Era I building cards face up in the top row.
     *
     * @param playerCount number of players in the game (2–5)
     */
    public void setup(int playerCount) {
        TribeCardFactory.TribeCardCollection tribeCards = TribeCardFactory.createAll();
        List<BuildingCard> allBuildingCards = BuildingCardFactory.createAll();

        tribeDeck.initializeDeck(tribeCards.regularCards(), tribeCards.finalEvents(), playerCount);
        buildingDeckEraI.initializeDeck(allBuildingCards, playerCount);
        buildingDeckEraII.initializeDeck(allBuildingCards, playerCount);
        buildingDeckEraIII.initializeDeck(allBuildingCards, playerCount);

        bottomRowTribe.addAll(tribeDeck.drawMultiple(playerCount + 1));
        topRowTribe.addAll(tribeDeck.drawMultiple(playerCount + 4));
        topRowBuilding.addAll(buildingDeckEraI.drawAll());
    }

    /**
     * This method is responsible for resolving the events present in the bottom row at the end of the round, it is called by the EndOfRoundPhaseHandler
     * at first it collects all the EventCard in the bottom row. The EventResolver sorts events by type and era,
     * then it calls the resolve method of each EventCard, passing the list of players as parameter, so that the EventCard can apply its effect on the players.
     */
    public List<EventResolutionDto> resolveEvents(List<Player> players){
        eventResolver.sortEvents(bottomRowTribe);
        return eventResolver.resolve(players);
    }

    /**
     * Solves all the events on the board. Called only by EndOfGamePhase
     *  EventResolver sorts events by era and type, moving sustenance event at the end of the list from all cards present on the board. Then resolves all the events <br>
     * <b>NOTE: </b> to create the list of all cards present on the board it puts bottomRow cards first because maybe there could be some ERA_II card
     */
    public List<EventResolutionDto> resolveAllEvents(List<Player> players){
        eventResolver.sortEvents(getAllTribeCardsOnBoard());
        return eventResolver.resolve(players);
    }

    /**
     * 1. discard all tribe cards from bottom row
     * 2. moves all tribe cards from top row to bottom row
     * 3. restores top row tribe cards, drawing from tribe deck
     * @param playerCount the number of players (used to determine how many cards to draw)
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
        if (!buildingDeckEraII.isEmpty()) {
            topRowBuilding.addAll(buildingDeckEraII.drawAll());
        } else if (!buildingDeckEraIII.isEmpty()) {
            topRowBuilding.addAll(buildingDeckEraIII.drawAll());
        }
        // If all era decks are exhausted the top building row stays empty (end-game state)
    }

    /**
     * Checks whether a specific card is present in the top row (tribe or building).
     *
     * @param cardId the unique identifier of the card to search for
     * @return true if the card is found in the top row, false otherwise
     */
    public boolean topRowContainsCard(int cardId) {
        return Stream.of(topRowTribe, topRowBuilding)
                .flatMap(List::stream)
                .anyMatch(c -> c.getId() == cardId);
    }

    /**
     * Checks whether a specific card is present in the bottom row (tribe or building).
     *
     * @param cardId the unique identifier of the card to search for
     * @return true if the card is found in the bottom row, false otherwise
     */
    public boolean bottomRowContainsCard(int cardId) {
        return Stream.of(bottomRowTribe, bottomRowBuilding)
                .flatMap(List::stream)
                .anyMatch(c -> c.getId() == cardId);
    }

    /**
     * Finds a card by its unique identifier across all four board rows.
     *
     * @param cardId the unique identifier of the card to search for
     * @return an {@link Optional} containing the {@link Card} if found, or an empty Optional if not present on the board
     */
    public Optional<Card> findCardById(int cardId) {
        return getAllCardsOnBoard().stream()
                .filter(c -> c.getId() == cardId)
                .findFirst();
    }

    /**
     * Removes a card by its unique identifier from all four rows.
     * 
     * Searches through all tribe and building rows (top and bottom) and removes the first card
     * matching the given ID. Does nothing if no card with the specified ID is found.
     *
     * @param cardId the unique identifier of the card to remove
     */
    public void removeCard(int cardId) {
        Stream.of(topRowTribe, bottomRowTribe, topRowBuilding, bottomRowBuilding)
                .forEach(list -> list.removeIf(card -> card.getId() == cardId));
    }

    /**
     * Returns all tribe cards currently on the board (top and bottom rows).
     * 
     * The returned list contains bottom row cards first, followed by top row cards.
     *
     * @return a {@link List} of all {@link TribeCard}s present on the board
     */
    public List<TribeCard> getAllTribeCardsOnBoard() {
        List<TribeCard> allCards = new ArrayList<>(bottomRowTribe);
        allCards.addAll(topRowTribe);
        return allCards;
    }

    /**
     * Returns all cards (tribe and building) currently on the board (top and bottom rows).
     * 
     * The returned list contains cards in the following order: bottom tribe row, top tribe row,
     * bottom building row, top building row.
     *
     * @return a {@link List} of all {@link Card}s present on the board
     */
    public List<Card> getAllCardsOnBoard() {
        List<Card> allCards = new ArrayList<>(bottomRowTribe);
        allCards.addAll(topRowTribe);
        allCards.addAll(bottomRowBuilding);
        allCards.addAll(topRowBuilding);
        return allCards;
    }

    public List<TribeCard>    getTopRowTribe()      { return topRowTribe; }
    public List<TribeCard>    getBottomRowTribe()   { return bottomRowTribe; }
    public List<BuildingCard> getTopRowBuilding()   { return topRowBuilding; }
    public List<BuildingCard> getBottomRowBuilding(){ return bottomRowBuilding; }

    public TribeDeck getTribeDeck() {
        return tribeDeck;
    }

    public Era getCurrentEra() {
        return tribeDeck.getCurrentEra();
    }
}
