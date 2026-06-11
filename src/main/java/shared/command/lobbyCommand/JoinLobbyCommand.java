package shared.command.lobbyCommand;

/**
 * Lobby command: request to join an existing lobby.
 *
 * @param playerName the sender player's name
 * @param lobbyId    identifier of the lobby to join
 */
public record JoinLobbyCommand(String playerName, String lobbyId) implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
