package shared.command.lobbyCommand;

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
