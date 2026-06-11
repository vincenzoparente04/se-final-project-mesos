package shared.command.gameCommand;

/**
 * Game command: the player draws/selects a card from the offer.
 *
 * @param playerName the sender player's name
 * @param cardId     identifier of the chosen card
 */
public record DrawCardCommand(String playerName, int cardId) implements GameCommand {

    @Override
    public void accept(GameCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }
}
