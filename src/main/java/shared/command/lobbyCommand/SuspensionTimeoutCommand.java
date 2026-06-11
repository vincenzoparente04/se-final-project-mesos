package shared.command.lobbyCommand;

import shared.command.ClientCommand;

/**
 * Server-internal lifecycle command. The {@code GameController} itself creates
 * it — scheduled by the suspension scheduler — when the suspension timer
 * expires. Enqueuing it instead of running the logic in the scheduled task
 * guarantees that closing the game (setWinners, setGameOver, notifyChange)
 * happens on the game thread, preserving the invariant "only the game thread
 * mutates the model".
 * <p>
 * It never travels over the wire: although it extends {@link LobbyCommand}
 * (hence {@link ClientCommand}, Serializable), it is created and consumed within
 * the same JVM.
 */
public record SuspensionTimeoutCommand() implements LobbyCommand {

    @Override
    public void accept(LobbyCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getPlayerName() {
        return null; // synthetic command, not associated to a single player
    }
}
