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

    default void setGameQueue(BlockingQueue<GameCommand> queue) {}
}
