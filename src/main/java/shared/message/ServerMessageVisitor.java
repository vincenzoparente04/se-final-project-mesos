package shared.message;

/**
 * Visitor over {@link ServerMessage}, implemented by the client side to react to
 * each kind of server message (state update, errors, game over, lobby
 * list/state, game start, heartbeat, leaderboard, event outcome) without
 * resorting to {@code instanceof}.
 */
public interface ServerMessageVisitor {
    /** @param msg new game-state snapshot to render */
    void visit(StateMessage msg);

    /** @param msg application error to show to the user */
    void visit(ErrorMessage msg);

    /** @param msg game-over notification with winners and optional scoring */
    void visit(GameOverMessage msg);

    /** @param msg list of available lobbies */
    void visit(LobbyListMessage msg);

    /** @param msg updated state of the player's lobby */
    void visit(LobbyStateMessage msg);

    /** @param msg game-start signal (lobby → game transition) */
    void visit(GameStartingMessage msg);

    /** @param msg server liveness proof (handled by the liveness channel) */
    void visit(HeartbeatMessage msg);

    /** @param msg final leaderboard and the player's rank/points */
    void visit(LeaderboardMessage msg);

    /**
     * Default no-op so existing client implementations keep compiling.
     * Override once the client task starts using the event-resolved data
     * to render the explanatory screen.
     *
     * @param msg structured outcome of an event-card resolution
     */
    default void visit(EventResolvedMessage msg) {}
}
