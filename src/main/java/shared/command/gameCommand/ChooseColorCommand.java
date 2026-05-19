package shared.command.gameCommand;

public record ChooseColorCommand(String playerName, String color) implements GameCommand {

    @Override
    public void accept(GameCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
