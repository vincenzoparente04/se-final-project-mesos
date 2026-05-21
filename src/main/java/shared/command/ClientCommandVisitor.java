package shared.command;

import shared.command.gameCommand.GameCommand;
import shared.command.gameCommand.GameCommandVisitor;
import shared.command.lobbyCommand.HeartbeatCommand;
import shared.command.lobbyCommand.LobbyCommand;
import shared.command.lobbyCommand.LobbyCommandVisitor;

/**
 * Top-level visitor on {@link ClientCommand}: dispatches the three families
 * ({@link GameCommand}, {@link LobbyCommand}, {@link HeartbeatCommand})
 * without knowing the bottom implementationd. Used by network's endpoints to
 * choose bewteen enqueuing a game command or delegate to {@code LobbyManager} a
 * lobby command. {@code GameController} Uses it to dispatch between network events and
 * game commands.
 *
 * <p> Low-level implementations:  {@link GameCommandVisitor} and {@link LobbyCommandVisitor}.
 */
public interface ClientCommandVisitor {
    void visit(GameCommand cmd) throws Exception;
    void visit(LobbyCommand cmd) throws Exception;
    void visit(HeartbeatCommand cmd) throws Exception;
}
