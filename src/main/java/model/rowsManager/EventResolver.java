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
 * A specialized stateful visitor responsible for sequencing
 * and resolving event cards from a heterogeneous collection of tribe cards.
 * <p>
 * This class applies the <em>Visitor</em> design pattern to safely identify
 * and extract different types of event cards without relying on type casting
 * or {@code instanceof} checks. Its primary architectural responsibility is
 * to enforce the strict game rule that all standard events must be resolved
 * before any Sustenance events.
 * </p>
 * <b>Usage Lifecycle:</b>
 * <ol>
 * <li>Call {@link #sortEvents(List)} to populate and order the internal resolution queues.</li>
 * <li>Call {@link #resolve(List)} to trigger the effects and generate the data transfer objects (DTOs) for the clients.</li>
 * </ol>
 * @see model.rowsManager.CardVisitor
 * @see model.cards.eventCards.EventCard
 * @see model.cards.eventCards.SustenanceEventCard
 */
public class EventResolver implements CardVisitor {
    private List<EventCard> eventsToResolve;
    private List<SustenanceEventCard> sustenanceToResolve;

    public EventResolver() {
        this.eventsToResolve = new ArrayList<>();
        this.sustenanceToResolve = new ArrayList<>();
    }

    /**
     * EventResolver collects EventCard and SustenanceEventCard from a list of TribeCards, sorts them by type and
     * era, and resolves them in the correct order. Sustenance events are resolved after all other events.
     * It assumes that cards on the rows are already sorted by era (they keep the drawing order from tribe deck)
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
     * Executes the sequential resolution of all previously sorted event cards,
     * applying their programmatic effects to the provided list of players.
     *
     * @param players the list of players in the game.
     * @return an ordered list of {@link EventResolutionDto} representing the outcome
     * of each resolved event, ready to be broadcasted to the clients.
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
