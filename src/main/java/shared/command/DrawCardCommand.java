package shared.command;

/**
 * Command issued by a player to draw a card from the board during
 * the action phase.
 */
public record DrawCardCommand(String playerName, int cardId) implements GameCommand {

    @Override
    public void accept(CommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
