package shared.message;

public record GameStartingMessage() implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor handler) {
        handler.visit(this);
    }
}
