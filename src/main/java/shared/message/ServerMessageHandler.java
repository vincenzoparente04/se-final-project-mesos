package shared.message;

public interface ServerMessageHandler {
    void handle(StateMessage msg);
    void handle(ErrorMessage msg);
    void handle(GameOverMessage msg);
    void handle(LobbyListMessage msg);
    void handle(LobbyStateMessage msg);
    void handle(GameStartingMessage msg);
}
