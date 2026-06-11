package shared.command;

import shared.command.gameCommand.GameCommand;
import shared.command.gameCommand.GameCommandVisitor;
import shared.command.lobbyCommand.HeartbeatCommand;
import shared.command.lobbyCommand.LobbyCommand;
import shared.command.lobbyCommand.LobbyCommandVisitor;

/**
 * Top-level visitor over {@link ClientCommand}: dispatches the three families
 * ({@link GameCommand}, {@link LobbyCommand}, {@link HeartbeatCommand}) without
 * knowing the concrete implementations. The network endpoints use it to choose
 * whether to enqueue a game command or delegate a lobby command to the
 * {@code LobbyManager}; the {@code GameController} uses it to dispatch between
 * network events and game commands.
 *
 * <p>The second-level visitors are {@link GameCommandVisitor} and
 * {@link LobbyCommandVisitor}.
 */
public interface ClientCommandVisitor {
    /**
     * @param cmd game command, to be dispatched to the {@link GameCommandVisitor}
     * @throws Exception if handling the command fails
     */
    void visit(GameCommand cmd) throws Exception;

    /**
     * @param cmd lobby command, to be dispatched to the {@link LobbyCommandVisitor}
     * @throws Exception if handling the command fails
     */
    void visit(LobbyCommand cmd) throws Exception;

    /**
     * @param cmd application-level heartbeat from the client
     * @throws Exception if handling the command fails
     */
    void visit(HeartbeatCommand cmd) throws Exception;
}
