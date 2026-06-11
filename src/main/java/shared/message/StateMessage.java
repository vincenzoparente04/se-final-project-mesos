package shared.message;

import shared.dto.GameStateDto;

/**
 * Server→client message carrying the full snapshot of the game state; it is the
 * main message the client uses to update its view.
 *
 * @param state serializable snapshot of the game state
 */
public record StateMessage(GameStateDto state) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
