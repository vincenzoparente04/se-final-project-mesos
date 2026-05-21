package model.rowsManager;

import model.cards.buildingCards.BuildingCard;
import model.cards.TribeCard;
import model.cards.characterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.player.Player;
import shared.dto.event.EventResolutionDto;

import java.util.ArrayList;
import java.util.List;

/**
 * EventResolver is responsible for visiting and sorting event cards using the Visitor pattern.
 * It separates concerns by handling event resolution logic independently from the board management.
 */
public class EventResolver implements CardVisitor {
    private List<EventCard> eventsToResolve;
    private List<SustenanceEventCard> sustenanceToResolve;

    public EventResolver() {
        this.eventsToResolve = new ArrayList<>();
        this.sustenanceToResolve = new ArrayList<>();
    }

    /**
     * Sorts events from a list of tribe cards by type and era, moving sustenance events to the end.
     * Uses the Visitor pattern to classify cards.
     * @implNote EventResolver collects EventCard and SustenanceEventCard from a list of TribeCards, sorts them by type and era, and resolves them in the correct order.
     * Sustenance events are resolved after all other events. It assumes that cards on the rows are already sorted by era (they keep the drawing order from tribe deck)
     *
     * @param tribeCards the cards to sort and filter for events
     */
    public void sortEvents(List<TribeCard> tribeCards) {
        eventsToResolve.clear();
        sustenanceToResolve.clear();

        for (TribeCard card : tribeCards) {
            card.accept(this);
        }

        // Add sustenance events at the end
        eventsToResolve.addAll(sustenanceToResolve);
    }

    /**
     * Resolves all events in the sorted list and returns one
     * {@link EventResolutionDto} per event card resolved (in resolution
     * order). The DTOs are forwarded by the phase to the client via
     * {@code EventResolvedMessage}.
     *
     * @param players the list of players affected by the events
     */
    public List<EventResolutionDto> resolve(List<Player> players) {
        List<EventResolutionDto> resolutions = new ArrayList<>();
        for (EventCard event : eventsToResolve) {
            resolutions.add(event.resolve(players));
        }
        return resolutions;
    }

    @Override
    public void visit(CharacterCard card) {
        // CharacterCard does not have any event to resolve
    }

    @Override
    public void visit(EventCard card) {
        eventsToResolve.add(card);
    }

    @Override
    public void visit(SustenanceEventCard card) {
        sustenanceToResolve.add(card);
    }

    @Override
    public void visit(BuildingCard card) {
        // There are no building cards in tribe rows
    }
}
