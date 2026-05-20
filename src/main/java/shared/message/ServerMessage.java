package shared.message;

import java.io.Serializable;


/**
 * Message originated by the server and sent from the server to the client.
 */
public sealed interface ServerMessage extends Serializable
        permits StateMessage, ErrorMessage, GameOverMessage,
                LobbyListMessage, LobbyStateMessage, GameStartingMessage,
                EventResolvedMessage {
                LobbyListMessage, LobbyStateMessage, GameStartingMessage, HeartbeatMessage {

    void accept(ServerMessageVisitor visitor);
}
