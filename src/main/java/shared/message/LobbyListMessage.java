package shared.message;

import shared.dto.LobbyDto;

import java.util.List;

public record LobbyListMessage(List<LobbyDto> lobbies) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
