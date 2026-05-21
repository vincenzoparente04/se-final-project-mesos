package shared.message;

public record ErrorMessage(String message) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
