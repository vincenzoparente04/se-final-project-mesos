package shared.message;

public record GameStartingMessage() implements ServerMessage {
    @Override
    public void accept(ServerMessageHandler handler) {
        handler.handle(this);
    }
}
