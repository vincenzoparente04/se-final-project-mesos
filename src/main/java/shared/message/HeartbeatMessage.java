package shared.message;

/**
 * Heartbeat message sent periodically by the server to the client to signal that
 * the connection is still alive. Symmetric to
 * {@link shared.command.lobbyCommand.HeartbeatCommand} (client→server).
 *
 * <p>The client-side receiver treats this message as a plain liveness proof: it
 * calls {@link shared.liveness.LivenessSentinel#notifyInbound()} and performs no
 * other application action.
 */
public record HeartbeatMessage() implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) {
        visitor.visit(this);
    }
}
