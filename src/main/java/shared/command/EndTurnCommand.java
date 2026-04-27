package shared.command;

public record EndTurnCommand(String playerName) implements GameCommand {

    @Override
    public void accept(CommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
