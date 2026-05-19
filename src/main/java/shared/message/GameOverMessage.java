package shared.message;

import java.util.List;

public record GameOverMessage(List<String> winners) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
