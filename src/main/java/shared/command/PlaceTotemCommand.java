package shared.command;

public record PlaceTotemCommand(String playerName, char tileId) implements GameCommand {

    @Override
    public void accept(CommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
