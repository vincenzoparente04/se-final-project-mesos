package shared.command.lobbyCommand;

/**
 * Visitor over {@link LobbyCommand}. Every method has a no-op default so that
 * implementors can override only the cases they care about: the
 * {@code LobbyManager} handles the user-facing menu commands (create/join/
 * list/leave), the registrations and the disconnections (register-socket/
 * register-rmi/disconnect); the {@code GameController} handles the lifecycle
 * commands that mutate the model (disconnect/reconnect/suspension-timeout).
 */
public interface LobbyCommandVisitor {
    /**
     * @param cmd request to create a new lobby
     * @throws Exception if handling the command fails
     */
    default void visit(CreateLobbyCommand cmd) throws Exception {}

    /**
     * @param cmd request to join an existing lobby
     * @throws Exception if handling the command fails
     */
    default void visit(JoinLobbyCommand cmd) throws Exception {}

    /**
     * @param cmd request for the list of available lobbies
     * @throws Exception if handling the command fails
     */
    default void visit(ListLobbiesCommand cmd) throws Exception {}

    /**
     * @param cmd voluntary leave from a lobby or a finished game
     * @throws Exception if handling the command fails
     */
    default void visit(LeaveCommand cmd) throws Exception {}

    /**
     * @param cmd server-internal notification of a player disconnecting mid-game
     * @throws Exception if handling the command fails
     */
    default void visit(PlayerDisconnectedCommand cmd) throws Exception {}

    /**
     * @param cmd server-internal notification of a player reconnecting mid-game
     * @throws Exception if handling the command fails
     */
    default void visit(PlayerReconnectedCommand cmd) throws Exception {}

    /**
     * @param cmd expiry of the game suspension timer (server-internal)
     * @throws Exception if handling the command fails
     */
    default void visit(SuspensionTimeoutCommand cmd) throws Exception {}

    /**
     * @param cmd server-internal registration of a socket-transport player
     * @throws Exception if handling the command fails
     */
    default void visit(RegisterSocketPlayerCommand cmd) throws Exception {}

    /**
     * @param cmd server-internal registration of an RMI-transport player
     * @throws Exception if handling the command fails
     */
    default void visit(RegisterRmiPlayerCommand cmd) throws Exception {}

    /**
     * @param cmd server-internal disconnection request, enqueued by the liveness
     *            sentinels and the transport handlers
     * @throws Exception if handling the command fails
     */
    default void visit(LobbyDisconnectCommand cmd) throws Exception {}
}
