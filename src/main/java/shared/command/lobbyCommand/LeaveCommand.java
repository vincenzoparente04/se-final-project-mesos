package shared.command.lobbyCommand;

/**
 * Command to leave a lobby or an ongoing game.
 * <p>
 * Bifurcates on the server side:
 * - If player is in a lobby: removes them from the lobby
 * - If player is in a game (END_OF_GAME phase): removes them from the game
 * - Otherwise: sends error
 * <p>
 * Player remains connected after leaving.
 *
 * @param playerName the sender player's name
 */
public record LeaveCommand(String playerName) implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}


