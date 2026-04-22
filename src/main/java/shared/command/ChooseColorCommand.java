package shared.command;

/**
 * Command issued by a player to select a totem colour during the
 * colour-choosing phase.
 */
public record ChooseColorCommand(String playerName, String color) implements GameCommand {

    @Override
    public void accept(CommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
