package shared.message;

import java.io.Serializable;


/**
 * Message originated by the server and sent from the server to the client.
 */
public interface ServerMessage extends Serializable {
    void accept(ServerMessageVisitor visitor);
}
