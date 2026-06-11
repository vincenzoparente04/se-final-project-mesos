package shared.message;

import java.io.Serializable;


/**
 * Root of the server→client message hierarchy. Every message is
 * {@link Serializable} (it travels over socket/RMI) and applies the Visitor
 * pattern: {@link #accept(ServerMessageVisitor)} double-dispatches on the
 * concrete subtype, so that the client side can react without {@code instanceof}.
 *
 * <p>Unlike {@link shared.command.ClientCommand}, message handling is purely
 * local to the client and does not propagate exceptions: neither {@code accept}
 * nor the {@link ServerMessageVisitor} methods declare {@code throws}.
 */
public interface ServerMessage extends Serializable {
    /**
     * Double dispatch towards the server-message visitor.
     *
     * @param visitor visitor handling the concrete subtype
     */
    void accept(ServerMessageVisitor visitor);
}
