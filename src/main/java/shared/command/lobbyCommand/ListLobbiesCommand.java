package shared.command.lobbyCommand;

/**
 * Lobby command: request for the list of available lobbies.
 *
 * @param playerName the sender player's name
 */
public record ListLobbiesCommand(String playerName) implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
