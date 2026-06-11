package shared.command.gameCommand;

/**
 * Game command: the player declares their turn over.
 *
 * @param playerName the sender player's name
 */
public record EndTurnCommand(String playerName) implements GameCommand {

    @Override
    public void accept(GameCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
