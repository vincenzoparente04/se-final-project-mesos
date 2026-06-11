package shared.message;

/**
 * Server→client message signalling the game start: the lobby is full and the
 * client must switch from the lobby screen to the game screen.
 */
public record GameStartingMessage() implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
