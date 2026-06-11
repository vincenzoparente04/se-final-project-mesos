package shared.command.gameCommand;

/**
 * Game command: the player chooses their colour during setup.
 *
 * @param playerName the sender player's name
 * @param color      the chosen colour (colour enum name)
 */
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
