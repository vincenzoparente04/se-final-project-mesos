package shared.command.lobbyCommand;

import shared.command.ClientCommand;

/**
 * Server-internal lifecycle command. The {@code LobbyManager} creates it when
 * it detects the disconnection of a player taking part in an active game and
 * enqueues it on the session queue, where the {@code GameController} processes
 * it via {@link LobbyCommandVisitor#visit(PlayerDisconnectedCommand)}, applying
 * the required mutations on the model (set disconnected, view removal, turn skip
 * if it was theirs).
 * <p>
 * It never travels over the wire: although it extends {@link LobbyCommand}
 * (hence {@link ClientCommand}, Serializable), it is created and consumed within
 * the same JVM.
 *
 * @param playerName name of the disconnected player
 */
public record PlayerDisconnectedCommand(String playerName) implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
