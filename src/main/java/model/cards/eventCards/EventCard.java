package model.cards.eventCards;

import model.rowsManager.CardVisitor;
import model.cards.TribeCard;
import model.enums.Era;
import model.player.Player;
import shared.dto.event.EventResolutionDto;

import java.util.List;

/**
 * Abstract base class for all global event cards within the game lifecycle.
 * <p>
 * Unlike character or building cards, event cards represent transient game phases that impact
 * all active participants simultaneously. They encapsulate game-wide resolution logic triggered
 * at the end of a round. This class defines the entry point for phase execution and acts as a
 * data producer for network synchronization by compiling phase deltas into transportable DTOs.
 * </p>
 */
public abstract class EventCard extends TribeCard {

    /**
     * Constructs a new base {@code EventCard} with specific era and asset bindings.
     *
     * @param id the unique sequential identifier assigned by the event deck factory
     * @param era the chronological {@link Era} during which this event can occur
     * @param playerCount the minimum player threshold required to include this event in the active deck
     * @param imagePath the resource path for the event's front graphical asset
     * @param backImagePath the resource path for the standard event deck back graphical asset
     */
    public EventCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    /**
     * <p>
     * <b>Design Note:</b> This method is explicitly implemented as a no-op (empty return)
     * because event cards are processed globally and are never structurally enrolled or
     * permanently attached to an individual player's internal tribe layout.
     * </p>
     *
     * @param player the player attempting a registration sequence
     */
    @Override
    public void registerToTribe(Player player) {
        return;
    }

    /**
     * Executes the global event resolution logic across all active game participants.
     * <p>
     * This abstract method orchestrates the phase-specific computations (e.g., executing
     * resource subtractions, validating building immunity flags, or compiling demographic
     * scaling bonuses). It aggregates the resulting mutations into an {@link EventResolutionDto},
     * allowing the server-side {@code EndOfRoundPhase} engine to broadcast synchronized
     * state updates to all clients via network messages.
     * </p>
     *
     * @param players the unmodifiable or active lifecycle tracking list of {@link Player} instances
     * @return a structured, network-ready {@link EventResolutionDto} capturing the structural
     * food/prestige deltas and localized summary descriptions for each player
     */
    public abstract EventResolutionDto resolve(List<Player> players);

    /**
     * <p>
     * Routes the execution path directly to the event card specialization of the visitor.
     * This ensures type-safe double-dispatch processing by deck evaluation or serialization
     * subsystems without resorting to manual reflection or casting routines.
     * </p>
     *
     * @param visitor the {@link CardVisitor} performing structural operations on this card
     */
    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}