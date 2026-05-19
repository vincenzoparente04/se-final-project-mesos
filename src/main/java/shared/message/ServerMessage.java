package shared.message;

import java.io.Serializable;


/**
 * Message originated by the server and sent from the server to the client.
 */
public sealed interface ServerMessage extends Serializable
        permits StateMessage, ErrorMessage, GameOverMessage,
                LobbyListMessage, LobbyStateMessage, GameStartingMessage {

    void accept(ServerMessageVisitor visitor);
}
