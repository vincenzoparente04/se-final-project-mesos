package shared.message;

import shared.dto.event.EventResolutionDto;

/**
 * Server → client notification emitted once per resolved event card during
 * {@code EndOfRoundPhase} (and {@code EndOfGamePhase} for residual events).
 * Carries the structured {@link EventResolutionDto} so that the client can
 * render an explanatory screen / popup describing what happened and how
 * each player's food/prestige changed.
 */
public record EventResolvedMessage(EventResolutionDto resolution) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
