package shared.command.lobbyCommand;

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
