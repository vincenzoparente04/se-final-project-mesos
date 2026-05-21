package shared.message;

import shared.dto.GameStateDto;

public record StateMessage(GameStateDto state) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
