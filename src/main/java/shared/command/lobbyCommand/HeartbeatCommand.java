package shared.command.lobbyCommand;

import shared.command.ClientCommand;
import shared.command.ClientCommandVisitor;

/**
 * Application-level heartbeat sent periodically by the client to the server to
 * signal that the connection is still alive.
 * <p>
 * The server keeps track of the timestamp of the last heartbeat received for
 * each player; a periodic task marks as disconnected every player whose last
 * heartbeat is older than a configurable threshold.
 *
 * @param playerName the sender player's name
 */
public record HeartbeatCommand(String playerName) implements ClientCommand {

    @Override
    public void accept(ClientCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}

