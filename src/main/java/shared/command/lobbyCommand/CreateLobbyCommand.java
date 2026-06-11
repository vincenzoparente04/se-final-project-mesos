package shared.command.lobbyCommand;

/**
 * Lobby command: request to create a new lobby.
 *
 * @param playerName    name of the player creating the lobby (becomes host)
 * @param playersNumber number of players expected for the game
 */
public record CreateLobbyCommand(String playerName, int playersNumber) implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
