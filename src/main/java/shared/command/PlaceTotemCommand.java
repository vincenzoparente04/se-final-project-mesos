package shared.command;

/**
 * Command issued by a player to place their totem on an offer tile
 * during the placement phase.
 */
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
