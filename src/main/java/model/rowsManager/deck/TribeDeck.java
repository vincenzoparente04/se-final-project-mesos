package model.rowsManager.deck;

import model.cards.TribeCard;
import model.enums.Era;

import java.util.*;
import java.util.stream.Collectors;

public class TribeDeck {
    private Deque<TribeCard> cards = new ArrayDeque<>();
    //private Deque<TribeCard> cards;

    private Era currentEra;

    // -- setup --
    /**
     * Builds the Tribe deck according to the game rules (Setup, step 3).
     * <p>
     * Called by RowsManager.setup() which obtains the two lists from TribeCardFactory:
     *   - regularCards  -> TribeCardFactory.createAll().regularCards()
     *   - finalEvents   -> TribeCardFactory.createAll().finalEvents()
     * <p>
     * Assembly order (top -> bottom):
     *   Era I (shuffled) | Era II (shuffled) | Era III (shuffled) | Final Events (any order)
     *
     * @param regularCards  all non-final TribeCards (characters + regular events)
     * @param finalEvents   the 2 Final Event cards, placed at the bottom in any order
     * @param playerCount   number of players in the game (2–5)
     */
    public void initializeDeck(List<TribeCard> regularCards, List<TribeCard> finalEvents, int playerCount) {

        // Discard cards not eligible for this player count.
        // Final Events always have minPlayers == 2 so they are always included.
        List<TribeCard> eligible = regularCards.stream()
                .filter(c -> c.getMinPlayerCount() <= playerCount)
                .collect(Collectors.toList());

        // Divide regular cards by era — event cards are mixed with characters
        // of the same era because they share the same era back (rules step 3).
        List<TribeCard> eraI   = filterByEra(eligible, Era.ERA_I);
        List<TribeCard> eraII  = filterByEra(eligible, Era.ERA_II);
        List<TribeCard> eraIII = filterByEra(eligible, Era.ERA_III);

        // Shuffle each era sub-deck independently, final events in any order
        Collections.shuffle(eraI);
        Collections.shuffle(eraII);
        Collections.shuffle(eraIII);
        List<TribeCard> shuffledFinalEvents = new ArrayList<>(finalEvents);
        Collections.shuffle(shuffledFinalEvents);
        //Collections.shuffle(finalEvents);

        // Assemble: Era I on top, Final Events at the very bottom
        cards = new ArrayDeque<>();
        for (TribeCard c : shuffledFinalEvents)       cards.addLast(c);
        //for (TribeCard c : finalEvents)       cards.addLast(c);
        for (int i = eraIII.size() - 1; i >= 0; i--) cards.addFirst(eraIII.get(i));
        for (int i = eraII.size()  - 1; i >= 0; i--) cards.addFirst(eraII.get(i));
        for (int i = eraI.size()   - 1; i >= 0; i--) cards.addFirst(eraI.get(i));

        currentEra = Era.ERA_I;
    }


    /**
     * Draws a card from the top of the deck and updates the current Era based on the drawn card.
     * @return the drawn TribeCard
     */
    public TribeCard draw(){
        if (cards.isEmpty()) return null;
        TribeCard drawn = cards.pop();
        currentEra = drawn.getEra();
        return drawn;
    }

    /**
     * Draws multiple cards from the deck.
     * @return a list of drawn TribeCards
     */
    public List<TribeCard> drawMultiple(int count){
        List<TribeCard> drawn = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TribeCard card = draw();
            if (card == null) break; // deck exhausted
            drawn.add(card);
        }
        return drawn;
    }

    // -- state --

    /**
     * Determines whether this deck is empty.
     *
     * @return true if no cards remain in the deck, false otherwise
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * Filters a list of tribe cards by era.
     *
     * @param list the list of cards to filter
     * @param era the era to match
     * @return a new list containing only cards from the specified era
     */
    private List<TribeCard> filterByEra(List<TribeCard> list, Era era) {
        return list.stream()
                .filter(c -> c.getEra() == era)
                .collect(Collectors.toList());
    }

    /**
     * Returns the number of cards remaining in this deck.
     *
     * @return the size of the deck
     */
    public int size() {
        return cards.size();
    }

    /**
     * Returns the current era based on the last drawn card.
     *
     * @return the {@link Era} of the last drawn card
     */
    public Era getCurrentEra(){
        return currentEra;
    }
}
