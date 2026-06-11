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

    String getName();

    VirtualView getView();

    /**
     * Wires the game session's command queue to the transport layer so that
     * in-game commands from this player are routed to the right session.
     * The default is a no-op; transport-specific implementations override
     * this to forward the queue to their command reader.
     *
     * @param queue the game command queue of the session this player has joined
     */
    default void setGameQueue(BlockingQueue<GameCommand> queue) {}
}
