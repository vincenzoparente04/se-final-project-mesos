package shared.message;

public record ErrorMessage(String message) implements ServerMessage {
    @Override
    public void accept(ServerMessageHandler handler) {
        handler.handle(this);
    }
}
