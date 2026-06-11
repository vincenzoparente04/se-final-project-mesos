package shared.command;

import java.io.Serializable;

/**
 * Root of the client→server command hierarchy. Every command is
 * {@link Serializable} because it may travel over the wire (socket/RMI) and
 * applies the double-dispatch Visitor pattern: {@link #accept(ClientCommandVisitor)}
 * selects the command family without the caller knowing the concrete subtype.
 *
 * <p>The subtypes split into two families, each with its own second-level
 * visitor — {@link shared.command.gameCommand.GameCommand} with
 * {@link shared.command.gameCommand.GameCommandVisitor}, and
 * {@link shared.command.lobbyCommand.LobbyCommand} with
 * {@link shared.command.lobbyCommand.LobbyCommandVisitor} — plus
 * {@link shared.command.lobbyCommand.HeartbeatCommand}, handled directly at the
 * top level.
 */
public interface ClientCommand extends Serializable {

    /**
     * Double dispatch towards the top-level visitor.
     *
     * @param visitor visitor handling the command family
     * @throws Exception if handling the command fails (propagated by the
     *                   handlers: invalid game state, network I/O, etc.)
     */
    void accept(ClientCommandVisitor visitor) throws Exception;

    /**
     * @return the sender player's name, or {@code null} for synthetic commands
     *         not tied to a single player (e.g.
     *         {@link shared.command.lobbyCommand.SuspensionTimeoutCommand})
     */
    String getPlayerName();
}
