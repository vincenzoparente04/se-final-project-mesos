package model.rowsManager.deck;

import model.cards.buildingCards.BuildingCard;
import model.enums.Era;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages a building card deck for a specific era.
 * 
 * Each era (I, II, III) has its own separate building deck. The deck is initialized
 * based on player count and holds the building cards available for that era.
 * 
 * @see Era
 * @see BuildingCard
 */
public class BuildingDeck {
    private Era era;
    private List<BuildingCard> cards;

    /**
     * Number of Building cards to use per Era based on player count (Setup, step 6).
     * <p>
     *  Players | Era I | Era II | Era III
     *  --------|-------|--------|--------
     *     2    |   1   |   2    |    3
     *     3    |   2   |   2    |    4
     *     4    |   2   |   3    |    4
     *     5    |   2   |   3    |    5
     * <p>
     * Indexed as [playerCount - 2][eraColumn]
     */
    private static final int[][] CARD_COUNT_TABLE = {
            {1, 2, 3},  // 2 players
            {2, 2, 4},  // 3 players
            {2, 3, 4},  // 4 players
            {2, 3, 5},  // 5 players
    };

    /**
     * Constructs a BuildingDeck for the specified era.
     *
     * @param era the {@link Era} this deck belongs to
     */
    public BuildingDeck(Era era) {
        this.era = era;
        this.cards = new ArrayList<>();
    }

    // -- setup --

    /**
     * Builds this era's Building deck according to the game rules (Setup, step 6).
     * <p>
     * Called by RowsManager.setup() which passes the full list from BuildingCardFactory.createAll().
     * This method:
     *   1. Filters cards belonging to this deck's Era and eligible for the player count
     *   2. Shuffles them
     *   3. Keeps only the number of cards specified by the player-count table;
     *      the rest are discarded (returned to the box in physical terms)
     *
     * @param allCards    the full list of BuildingCards from BuildingCardFactory.createAll()
     * @param playerCount number of players in the game (2–5)
     */
    public void initializeDeck(List<BuildingCard> allCards, int playerCount) {
        List<BuildingCard> eligible = allCards.stream()
                .filter(c -> c.getEra() == this.era)
                .filter(c -> c.getMinPlayerCount() <= playerCount)
                .collect(Collectors.toList());

        Collections.shuffle(eligible);

        int needed = getCardCountForEra(playerCount);
        int take = Math.min(needed, eligible.size());

        this.cards = new ArrayList<>(eligible.subList(0, take));
    }

    /**
     * Adds a building card to this deck.
     *
     * @param card the building card to add
     */
    public void addCard(BuildingCard card) {
        cards.add(card);
    }

    /**
     * Returns all cards in this deck and clears it.
     * 
     * Used at the beginning of each era to populate the top building row.
     *
     * @return a {@link List} of all {@link BuildingCard}s in this deck
     */
    public List<BuildingCard> drawAll() {
        List<BuildingCard> all = new ArrayList<>(cards);
        cards.clear();
        return all;
    }

    /**
     * Determines whether this deck is empty.
     *
     * @return true if no cards remain in the deck, false otherwise
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Returns the era associated with this deck.
     *
     * @return the {@link Era} of this deck
     */
    public Era getEra() {
        return era;
    }

    // -- helpers --

    /**
     * Retrieves the number of cards required for this era based on player count.
     * 
     * Uses the {@link #CARD_COUNT_TABLE CARD_COUNT_TABLE} to determine how many
     * building cards should be used for this era given the number of players.
     *
     * @param playerCount the number of players in the game (2–5)
     * @return the number of cards to use for this era
     */
    private int getCardCountForEra(int playerCount) {
        int row = playerCount - 2;
        int col = switch (era) {
            case ERA_I   -> 0;
            case ERA_II  -> 1;
            case ERA_III -> 2;
        };
        return CARD_COUNT_TABLE[row][col];
    }
}
