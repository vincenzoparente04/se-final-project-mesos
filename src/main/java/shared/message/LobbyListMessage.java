package shared.message;

import shared.dto.LobbyDto;

import java.util.List;

/**
 * Server→client message with the list of available lobbies, in response to
 * {@link shared.command.lobbyCommand.ListLobbiesCommand}.
 *
 * @param lobbies the currently available lobbies
 */
public record LobbyListMessage(List<LobbyDto> lobbies) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
