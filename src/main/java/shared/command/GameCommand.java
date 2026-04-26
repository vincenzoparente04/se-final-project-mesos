package shared.command;

import java.io.Serializable;

/**
 * Marker interface for all game commands.
 * <p>
 * Commands travel from client to server — over the network in RMI mode,
 * or reconstructed from raw socket lines in socket mode — and are placed
 * into a central {@code BlockingQueue} for sequential processing by the
 * {@code GameThread}.
 * <p>
 * Implementing classes must be {@link Serializable} (guaranteed because
 * this interface extends it) so that RMI can marshal them transparently.
 * The {@link #accept(CommandVisitor)} method enables execution without
 * {@code instanceof} or switch: the concrete type dispatches itself to
 * the correct visitor overload.
 */
public interface GameCommand extends Serializable {

    /**
     * Dispatches this command to the correct visitor overload.
     * The visitor implementation holds the actual execution logic.
     *
     * @param visitor the executor that handles this command type
     * @throws Exception if the underlying action fails
     */
    void accept(CommandVisitor visitor) throws Exception;

    /** Returns the name of the player who issued this command. */
    String getPlayerName();
}
