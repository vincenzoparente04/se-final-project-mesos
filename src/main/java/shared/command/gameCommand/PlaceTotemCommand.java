package shared.command.gameCommand;

public record PlaceTotemCommand(String playerName, char tileId) implements GameCommand {

    @Override
    public void accept(GameCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
