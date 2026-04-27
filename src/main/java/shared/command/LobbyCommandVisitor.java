package shared.command;

public interface LobbyCommandVisitor {
    void visit(CreateLobbyCommand cmd) throws Exception;
    void visit(JoinLobbyCommand cmd) throws Exception;
    void visit(ListLobbiesCommand cmd) throws Exception;
}
