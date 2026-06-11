package shared.command.lobbyCommand;

import shared.command.ClientCommand;
import shared.command.ClientCommandVisitor;

/**
 * Family of commands concerning the lifecycle of lobbies and sessions
 * (create/join/list/leave, player registration, disconnections and
 * reconnections). It adds a second dispatch level towards
 * {@link LobbyCommandVisitor}, while the top level routes to
 * {@link ClientCommandVisitor#visit(LobbyCommand)}.
 *
 * <p>Several subtypes are <em>server-internal</em> and never travel over the
 * wire even though they are {@link java.io.Serializable}: they are created and
 * consumed within the same JVM to be processed serially on the
 * {@code LobbyManager} or {@code GameController} thread (e.g.
 * {@link PlayerDisconnectedCommand}, {@link PlayerReconnectedCommand},
 * {@link RegisterSocketPlayerCommand}, {@link RegisterRmiPlayerCommand},
 * {@link SuspensionTimeoutCommand}).
 */
public interface LobbyCommand extends ClientCommand {

    /**
     * Double dispatch towards the second-level lobby-command visitor.
     *
     * @param visitor visitor handling the concrete subtype
     * @throws Exception if handling the command fails
     */
    void accept(LobbyCommandVisitor visitor) throws Exception;

    @Override
    default void accept(ClientCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
