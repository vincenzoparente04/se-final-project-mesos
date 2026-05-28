package shared.message;

public interface ServerMessageVisitor {
    void visit(StateMessage msg);
    void visit(ErrorMessage msg);
    void visit(GameOverMessage msg);
    void visit(LobbyListMessage msg);
    void visit(LobbyStateMessage msg);
    void visit(GameStartingMessage msg);
    void visit(HeartbeatMessage msg);
    void visit(LeaderboardMessage msg);

    /**
     * Default no-op so existing client implementations keep compiling.
     * Override once the client task starts using the event-resolved data
     * to render the explanatory screen.
     */
    default void visit(EventResolvedMessage msg) {}
}
