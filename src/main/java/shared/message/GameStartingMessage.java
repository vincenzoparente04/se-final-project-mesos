package shared.message;

public record GameStartingMessage() implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
