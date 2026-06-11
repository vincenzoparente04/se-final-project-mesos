package shared.message;

import shared.dto.LobbyDto;

/**
 * Server→client message with the updated state of the lobby the player belongs
 * to (e.g. after a participant joins or leaves).
 *
 * @param lobby the current lobby state
 */
public record LobbyStateMessage(LobbyDto lobby) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
