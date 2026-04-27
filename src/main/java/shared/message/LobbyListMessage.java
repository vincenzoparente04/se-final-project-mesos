package shared.message;

import shared.dto.LobbyDto;

import java.util.List;

public record LobbyListMessage(List<LobbyDto> lobbies) implements ServerMessage {
    @Override
    public void accept(ServerMessageHandler handler) {
        handler.handle(this);
    }
}
