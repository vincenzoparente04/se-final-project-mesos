package shared.message;

/**
 * Server→client message notifying an application error (e.g. command not valid
 * in the current phase, non-existent lobby, name already in use).
 *
 * @param message error text intended for the user
 */
public record ErrorMessage(String message) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
