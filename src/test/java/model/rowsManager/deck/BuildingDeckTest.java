package model.rowsManager.deck;

import model.cards.buildingCards.BuildingCard;
import model.enums.Era;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BuildingDeckTest {

    @Test
    @DisplayName("constructor initializes empty deck with the given era")
    void constructorInitializesEmptyDeckWithEra() {
        BuildingDeck deck = new BuildingDeck(Era.ERA_II);

        assertTrue(deck.isEmpty());
        assertEquals(Era.ERA_II, deck.getEra());
    }

    @Test
    @DisplayName("addCard stores cards and drawAll returns them then clears deck")
    void addCardAndDrawAllWorkTogether() {
        BuildingDeck deck = new BuildingDeck(Era.ERA_I);
        BuildingCard c1 = mockCard(Era.ERA_I, 2);
        BuildingCard c2 = mockCard(Era.ERA_I, 2);

        deck.addCard(c1);
        deck.addCard(c2);

        List<BuildingCard> drawn = deck.drawAll();

        assertEquals(2, drawn.size());
        assertTrue(drawn.contains(c1));
        assertTrue(drawn.contains(c2));
        assertTrue(deck.isEmpty());
    }

    @Test
    @DisplayName("drawAll on empty deck returns empty list")
    void drawAllOnEmptyDeckReturnsEmptyList() {
        BuildingDeck deck = new BuildingDeck(Era.ERA_III);

        List<BuildingCard> drawn = deck.drawAll();

        assertTrue(drawn.isEmpty());
        assertTrue(deck.isEmpty());
    }

    @Test
    @DisplayName("initializeDeck keeps only cards of this era and allowed by player count")
    void initializeDeckFiltersByEraAndPlayerCount() {
        BuildingDeck deck = new BuildingDeck(Era.ERA_II);

        BuildingCard allowed1 = mockCard(Era.ERA_II, 2);
        BuildingCard allowed2 = mockCard(Era.ERA_II, 3);
        BuildingCard wrongEra = mockCard(Era.ERA_I, 2);
        BuildingCard tooManyPlayers = mockCard(Era.ERA_II, 4);

        deck.initializeDeck(List.of(allowed1, allowed2, wrongEra, tooManyPlayers), 3);

        List<BuildingCard> drawn = deck.drawAll();

        assertEquals(2, drawn.size());
        assertTrue(drawn.contains(allowed1));
        assertTrue(drawn.contains(allowed2));
        assertFalse(drawn.contains(wrongEra));
        assertFalse(drawn.contains(tooManyPlayers));
    }

    @Test
    @DisplayName("initializeDeck respects per-era card count table")
    void initializeDeckRespectsCardCountTable() {
        assertDeckSizeAfterInitialize(Era.ERA_I, 2, 1);
        assertDeckSizeAfterInitialize(Era.ERA_I, 5, 2);

        assertDeckSizeAfterInitialize(Era.ERA_II, 2, 2);
        assertDeckSizeAfterInitialize(Era.ERA_II, 4, 3);

        assertDeckSizeAfterInitialize(Era.ERA_III, 2, 3);
        assertDeckSizeAfterInitialize(Era.ERA_III, 5, 5);
    }

    @Test
    @DisplayName("initializeDeck uses all eligible cards when fewer than required")
    void initializeDeckUsesAllEligibleCardsIfNotEnough() {
        BuildingDeck deck = new BuildingDeck(Era.ERA_III);

        BuildingCard c1 = mockCard(Era.ERA_III, 2);
        BuildingCard c2 = mockCard(Era.ERA_III, 2);

        deck.initializeDeck(List.of(c1, c2), 5);

        List<BuildingCard> drawn = deck.drawAll();

        assertEquals(2, drawn.size());
        assertTrue(drawn.containsAll(List.of(c1, c2)));
    }

    private static BuildingCard mockCard(Era era, int minPlayerCount) {
        BuildingCard card = mock(BuildingCard.class);
        when(card.getEra()).thenReturn(era);
        when(card.getMinPlayerCount()).thenReturn(minPlayerCount);
        return card;
    }

    private static void assertDeckSizeAfterInitialize(Era era, int playerCount, int expectedSize) {
        BuildingDeck deck = new BuildingDeck(era);
        List<BuildingCard> pool = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            pool.add(mockCard(era, 2));
        }

        deck.initializeDeck(pool, playerCount);

        assertEquals(expectedSize, deck.drawAll().size());
    }
}