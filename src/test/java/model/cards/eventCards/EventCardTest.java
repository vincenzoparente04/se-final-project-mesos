package model.cards.eventCards;

import model.rowsManager.CardVisitor;
import model.enums.Era;
import model.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shared.dto.event.EventResolutionDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("EventCard Tests")
class EventCardTest {

    // A simple concrete subclass of EventCard for testing purposes.
    private static class TestEventCard extends EventCard {

        TestEventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
            super(id, era, playerCount, imagePath, backImagePath);
        }

        @Override
        public EventResolutionDto resolve(List<Player> players) {
            // No-op test implementation: return an empty DTO.
            return new EventResolutionDto("TEST", getEra().name(), getId(),
                    "test event", List.of());
        }
    }

    @Test
    @DisplayName("Accept delegates to CardVisitor.visit(EventCard)")
    void acceptDelegatesToVisitor() {
        CardVisitor visitor = mock(CardVisitor.class);
        EventCard card = new TestEventCard(42, Era.ERA_II, 4, "front.png", "back.png");

        card.accept(visitor);

        verify(visitor).visit(card);
    }


    @Test
    @DisplayName("Constructor stores id, era and minimum player count")
    void constructorStoresBaseFields() {
        EventCard card = new TestEventCard(7, Era.ERA_III, 5, "front.png", "back.png");

        assertEquals(7, card.getId());
        assertEquals(Era.ERA_III, card.getEra());
        assertEquals(5, card.getMinPlayerCount());
    }
}
