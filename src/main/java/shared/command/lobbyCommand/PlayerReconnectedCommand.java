package shared.command.lobbyCommand;

import network.server.core.VirtualView;

/**
 * Server-internal lifecycle command. The {@code LobbyManager} creates it upon a
 * reconnection (socket or RMI handshake with a name already present in an active
 * game) and enqueues it on the session queue, where the {@code GameController}
 * processes it via {@link LobbyCommandVisitor#visit(PlayerReconnectedCommand)}:
 * it registers the new view on the model, marks the player as reconnected and
 * forwards the current state.
 * <p>
 * <strong>It never travels over the wire</strong>: the {@code newView} component
 * references a {@link VirtualView} that is not {@link java.io.Serializable}, but
 * the record is built and consumed within the same JVM and is never passed to an
 * {@code ObjectOutputStream}. Extending {@link LobbyCommand} is purely for type
 * uniformity in the controller's command queue.
 *
 * @param playerName name of the reconnecting player
 * @param newView    new {@link VirtualView} to register on the model for the player
 */
public record PlayerReconnectedCommand(String playerName, VirtualView newView) implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
