package model.eventCards;

import model.board.CardVisitor;
import model.cards.eventCards.EventCard;
import model.enums.CardType;
import model.enums.Era;
import model.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("EventCard Tests")
class EventCardTest {

    // A simple concrete subclass of EventCard for testing purposes.
    private static class TestEventCard extends EventCard {

        TestEventCard(int id, Era era, int playerCount) {
            super(id, era, playerCount);
        }

        @Override
        public void resolve(List<Player> players) {
            // No-op test implementation.
        }

        @Override
        public CardType getCardType() {
            return null;
        }
    }

    @Test
    @DisplayName("Accept delegates to CardVisitor.visit(EventCard)")
    void acceptDelegatesToVisitor() {
        CardVisitor visitor = mock(CardVisitor.class);
        EventCard card = new TestEventCard(42, Era.ERA_II, 4);

        card.accept(visitor);

        verify(visitor).visit(card);
    }


    @Test
    @DisplayName("Constructor stores id, era and minimum player count")
    void constructorStoresBaseFields() {
        EventCard card = new TestEventCard(7, Era.ERA_III, 5);

        assertEquals(7, card.getId());
        assertEquals(Era.ERA_III, card.getEra());
        assertEquals(5, card.getMinPlayerCount());
    }
}
