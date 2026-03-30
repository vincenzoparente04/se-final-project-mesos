package model.rowsManager.deck;

import model.cards.TribeCard;
import model.enums.Era;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TribeDeckTest {

    @Test
    @DisplayName("initializeDeck filters regular cards by player count and keeps final events")
    void initializeDeckFiltersRegularCardsButKeepsFinalEvents() {
        TribeDeck deck = new TribeDeck();

        TribeCard eraIAllowed = mockCard(Era.ERA_I, 2);
        TribeCard eraIIAllowed = mockCard(Era.ERA_II, 3);
        TribeCard excluded = mockCard(Era.ERA_I, 4);

        TribeCard final1 = mockCard(Era.ERA_III, 2);
        TribeCard final2 = mockCard(Era.ERA_III, 2);

        deck.initializeDeck(
                List.of(eraIAllowed, eraIIAllowed, excluded),
                List.of(final1, final2),
                3
        );

        assertEquals(4, deck.size());

        List<TribeCard> drawn = deck.drawMultiple(10);

        assertTrue(drawn.contains(eraIAllowed));
        assertTrue(drawn.contains(eraIIAllowed));
        assertFalse(drawn.contains(excluded));
        assertTrue(drawn.contains(final1));
        assertTrue(drawn.contains(final2));
    }

    @Test
    @DisplayName("draw returns cards in era order with final events at the bottom")
    void drawReturnsCardsInEraOrderWithFinalEventsLast() {
        TribeDeck deck = new TribeDeck();

        TribeCard eraI = mockNamedCard("eraI", Era.ERA_I, 2);
        TribeCard eraII = mockNamedCard("eraII", Era.ERA_II, 2);
        TribeCard eraIII = mockNamedCard("eraIII", Era.ERA_III, 2);
        TribeCard final1 = mockNamedCard("final1", Era.ERA_III, 2);
        TribeCard final2 = mockNamedCard("final2", Era.ERA_III, 2);

        deck.initializeDeck(
                List.of(eraI, eraII, eraIII),
                List.of(final1, final2),
                2
        );

        TribeCard first = deck.draw();
        TribeCard second = deck.draw();
        TribeCard third = deck.draw();
        TribeCard fourth = deck.draw();
        TribeCard fifth = deck.draw();

        assertSame(eraI, first);
        assertSame(eraII, second);
        assertSame(eraIII, third);
        assertTrue(fourth == final1 || fourth == final2);
        assertTrue(fifth == final1 || fifth == final2);
        assertNotSame(fourth, fifth);
    }

    @Test
    @DisplayName("draw returns null when deck is empty")
    void drawReturnsNullWhenEmpty() {
        TribeDeck deck = new TribeDeck();

        assertNull(deck.draw());
        assertTrue(deck.isEmpty());
    }

    @Test
    @DisplayName("draw updates current era to the drawn card era")
    void drawUpdatesCurrentEra() {
        TribeDeck deck = new TribeDeck();

        TribeCard eraI = mockCard(Era.ERA_I, 2);
        TribeCard eraII = mockCard(Era.ERA_II, 2);

        deck.initializeDeck(List.of(eraI, eraII), List.of(), 2);

        assertEquals(Era.ERA_I, deck.getCurrentEra());

        deck.draw();
        assertEquals(Era.ERA_I, deck.getCurrentEra());

        deck.draw();
        assertEquals(Era.ERA_II, deck.getCurrentEra());
    }

    @Test
    @DisplayName("drawMultiple returns at most requested cards and stops on deck exhaustion")
    void drawMultipleStopsWhenDeckIsExhausted() {
        TribeDeck deck = new TribeDeck();

        TribeCard c1 = mockCard(Era.ERA_I, 2);
        TribeCard c2 = mockCard(Era.ERA_I, 2);

        deck.initializeDeck(List.of(c1, c2), List.of(), 2);

        List<TribeCard> drawn = deck.drawMultiple(5);

        assertEquals(2, drawn.size());
        assertTrue(drawn.containsAll(List.of(c1, c2)));
        assertTrue(deck.isEmpty());
    }

    @Test
    @DisplayName("drawMultiple preserves draw order")
    void drawMultiplePreservesDrawOrder() {
        TribeDeck deck = new TribeDeck();

        TribeCard eraI = mockNamedCard("eraI", Era.ERA_I, 2);
        TribeCard eraII = mockNamedCard("eraII", Era.ERA_II, 2);
        TribeCard eraIII = mockNamedCard("eraIII", Era.ERA_III, 2);

        deck.initializeDeck(List.of(eraI, eraII, eraIII), List.of(), 2);

        List<TribeCard> drawn = deck.drawMultiple(3);

        assertEquals(List.of(eraI, eraII, eraIII), drawn);
    }

    @Test
    @DisplayName("size and isEmpty reflect deck state across draws")
    void sizeAndIsEmptyReflectDeckState() {
        TribeDeck deck = new TribeDeck();

        TribeCard c1 = mockCard(Era.ERA_I, 2);
        TribeCard c2 = mockCard(Era.ERA_I, 2);
        TribeCard c3 = mockCard(Era.ERA_II, 2);

        deck.initializeDeck(List.of(c1, c2, c3), List.of(), 2);

        assertEquals(3, deck.size());
        assertFalse(deck.isEmpty());

        deck.draw();
        assertEquals(2, deck.size());

        deck.drawMultiple(2);
        assertEquals(0, deck.size());
        assertTrue(deck.isEmpty());
    }

    @Test
    @DisplayName("initializeDeck with no eligible regular cards still keeps final events")
    void initializeDeckWithNoEligibleRegularCardsStillKeepsFinalEvents() {
        TribeDeck deck = new TribeDeck();

        TribeCard excluded1 = mockCard(Era.ERA_I, 4);
        TribeCard excluded2 = mockCard(Era.ERA_II, 5);
        TribeCard final1 = mockCard(Era.ERA_III, 2);
        TribeCard final2 = mockCard(Era.ERA_III, 2);

        deck.initializeDeck(List.of(excluded1, excluded2), List.of(final1, final2), 3);

        List<TribeCard> drawn = deck.drawMultiple(10);

        assertEquals(2, drawn.size());
        assertTrue(drawn.containsAll(List.of(final1, final2)));
    }

    private static TribeCard mockCard(Era era, int minPlayerCount) {
        TribeCard card = mock(TribeCard.class);
        when(card.getEra()).thenReturn(era);
        when(card.getMinPlayerCount()).thenReturn(minPlayerCount);
        return card;
    }

    private static TribeCard mockNamedCard(String name, Era era, int minPlayerCount) {
        TribeCard card = mock(TribeCard.class, name);
        when(card.getEra()).thenReturn(era);
        when(card.getMinPlayerCount()).thenReturn(minPlayerCount);
        return card;
    }
}