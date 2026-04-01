package model.rowsManager;

import model.cards.TribeCard;
import model.cards.charachterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class EventResolverTest {

    private EventResolver eventResolver;
    private List<Player> players;

    @BeforeEach
    void setUp() {
        eventResolver = new EventResolver();
        players = List.of(mock(Player.class));
    }

    @Test
    @DisplayName("Event cards are resolved in the order they were sorted, with sustenance events at the end")
    void sortEventsPlacesSustenanceAtTheEndPreservingRelativeOrder() {
        EventCard event1 = mock(EventCard.class);
        EventCard event2 = mock(EventCard.class);
        SustenanceEventCard sust1 = mock(SustenanceEventCard.class);
        SustenanceEventCard sust2 = mock(SustenanceEventCard.class);
        CharacterCard character = mock(CharacterCard.class);

        stubAccept(event1);
        stubAccept(event2);
        stubAccept(sust1);
        stubAccept(sust2);
        stubAccept(character);

        eventResolver.sortEvents(List.of(sust1, event1, character, sust2, event2));
        eventResolver.resolve(players);

        InOrder inOrder = inOrder(event1, event2, sust1, sust2);
        inOrder.verify(event1).resolve(players);
        inOrder.verify(event2).resolve(players);
        inOrder.verify(sust1).resolve(players);
        inOrder.verify(sust2).resolve(players);

        verify(character).accept(eventResolver);
    }

    @Test
    @DisplayName("Sorting new events clears previous state, so only the most recently sorted events are resolved")
    void sortEventsClearsPreviousStateBeforeCollectingNewEvents() {
        EventCard oldEvent = mock(EventCard.class);
        EventCard newEvent = mock(EventCard.class);

        stubAccept(oldEvent);
        stubAccept(newEvent);

        eventResolver.sortEvents(List.of(oldEvent));
        eventResolver.sortEvents(List.of(newEvent));
        eventResolver.resolve(players);

        verify(oldEvent, never()).resolve(players);
        verify(newEvent).resolve(players);
    }

    @Test
    @DisplayName("Resolving with no events does not throw an exception")
    void resolveWithEmptyInputDoesNotThrow() {
        assertDoesNotThrow(() -> {
            eventResolver.sortEvents(List.of());
            eventResolver.resolve(players);
        });
    }

    private void stubAccept(TribeCard card) {
        doAnswer(invocation -> {
            CardVisitor visitor = invocation.getArgument(0);
            if (card instanceof SustenanceEventCard sustenanceCard) {
                visitor.visit(sustenanceCard);
            } else if (card instanceof EventCard eventCard) {
                visitor.visit(eventCard);
            } else if (card instanceof CharacterCard characterCard) {
                visitor.visit(characterCard);
            }
            return null;
        }).when(card).accept(any(CardVisitor.class));
    }
}