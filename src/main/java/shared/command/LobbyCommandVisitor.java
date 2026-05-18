package shared.command;

/**
 * Visitor on {@link LobbyCommand}. Every method has a no-op default so that
 * implementors can override only the cases they care about: the
 * {@code LobbyManager} handles the user-facing menu commands (create/join/
 * list/leave), the {@code GameController} handles the lifecycle commands
 * that mutate the model (leave/disconnect/reconnect).
 */
public interface LobbyCommandVisitor {
    default void visit(CreateLobbyCommand cmd) throws Exception {}
    default void visit(JoinLobbyCommand cmd) throws Exception {}
    default void visit(ListLobbiesCommand cmd) throws Exception {}
    default void visit(LeaveCommand cmd) throws Exception {}
    default void visit(PlayerDisconnectedCommand cmd) throws Exception {}
    default void visit(PlayerReconnectedCommand cmd) throws Exception {}
    default void visit(SuspensionTimeoutCommand cmd) throws Exception {}
}
