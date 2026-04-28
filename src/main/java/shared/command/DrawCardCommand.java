package shared.command;

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
