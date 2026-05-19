package shared.message;

public interface ServerMessageVisitor {
    void visit(StateMessage msg);
    void visit(ErrorMessage msg);
    void visit(GameOverMessage msg);
    void visit(LobbyListMessage msg);
    void visit(LobbyStateMessage msg);
    void visit(GameStartingMessage msg);
}
