package model.rowsManager.deck;

import model.cards.buildingCards.BuildingCard;
import model.enums.Era;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BuildingDeck {
    private Era era;
    private List<BuildingCard> cards;

    /**
     * Number of Building cards to use per Era based on player count (Setup, step 6).
     *
     *  Players | Era I | Era II | Era III
     *  --------|-------|--------|--------
     *     2    |   1   |   2    |    3
     *     3    |   2   |   2    |    4
     *     4    |   2   |   3    |    4
     *     5    |   2   |   3    |    5
     *
     * Indexed as [playerCount - 2][eraColumn]
     */
    private static final int[][] CARD_COUNT_TABLE = {
            {1, 2, 3},  // 2 players
            {2, 2, 4},  // 3 players
            {2, 3, 4},  // 4 players
            {2, 3, 5},  // 5 players
    };

    public BuildingDeck(Era era) {
        this.era = era;
        this.cards = new ArrayList<>();
    }

    // -- setup --

    /**
     * Builds this era's Building deck according to the game rules (Setup, step 6).
     *
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

    public void addCard(BuildingCard card) {
        cards.add(card);
    }

    /**
     * Returns all cards in this deck and clears it.
     * Used at the beginning of each Era to populate the top row.
     */
    public List<BuildingCard> drawAll() {
        List<BuildingCard> all = new ArrayList<>(cards);
        cards.clear();
        return all;
    }

    // - state --
    public boolean isEmpty() {
        return cards.isEmpty();
    }
    public Era getEra(){return era;}

    // -- helpers --

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
