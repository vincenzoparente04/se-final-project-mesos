package shared.message;

import java.io.Serializable;

public sealed interface ServerMessage extends Serializable
        permits StateMessage, ErrorMessage, GameOverMessage,
                LobbyListMessage, LobbyStateMessage, GameStartingMessage {

    void accept(ServerMessageHandler handler);
}
