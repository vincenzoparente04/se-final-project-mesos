package shared.message;

import shared.dto.LobbyDto;

public record LobbyStateMessage(LobbyDto lobby) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor handler) {
        handler.visit(this);
    }
}
