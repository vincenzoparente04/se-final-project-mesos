package network.server.core;

import java.util.concurrent.BlockingQueue;

import shared.command.gameCommand.GameCommand;

/**
 * Server-side representation of a connected player, transport-agnostic.
 * Exposes the player name, the {@link VirtualView} used to send messages
 * back, and a hook to wire incoming in-game commands to the game session's
 * queue once the player joins an active game.
 */
public interface PlayerEntry {

    /** @return the player's server-wide name (their identity key) */
    String getName();

    /** @return the outbound {@link VirtualView} used to message this player */
    VirtualView getView();

    /**
     * Wires this player's incoming in-game commands to a running game session's
     * queue. Called when the player joins or reconnects to an active game; the
     * default no-op covers entries that never reach in-game state.
     *
     * @param queue the game session's command queue to forward in-game commands to
     */
    default void setGameQueue(BlockingQueue<GameCommand> queue) {}
}
