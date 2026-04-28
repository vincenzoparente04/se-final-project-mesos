package shared.command;

public interface CommandDispatcher {
    void onLobbyCommand(LobbyCommand cmd) throws Exception;
    void onGameCommand(GameCommand cmd) throws Exception;
}
