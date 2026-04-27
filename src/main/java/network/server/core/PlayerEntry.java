package server.core;

import shared.command.GameCommand;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Abstraction over one player's connection, regardless of transport protocol.
 * <p>
 * A {@link PlayerEntry} is created during the lobby phase (when the player
 * first connects) and carries the information needed both for sending
 * waiting notifications and for starting game-phase reading:
 * <ul>
 *   <li>{@link #getView()} — the {@link VirtualView} used to send state and
 *       errors to this player at any phase.</li>
 *   <li>{@link #activate(BlockingQueue, Consumer)} — called once by
 *       {@link Game#start()} to begin consuming incoming commands;
 *       for socket: starts the reading thread;
 *       for RMI: installs the disconnect handler.</li>
 * </ul>
 * Implementations: {@link server.socket.SocketPlayerEntry},
 * {@link server.rmi.RmiPlayerEntry}.
 */
public interface PlayerEntry {

    /** Returns the display name of the player. */
    String getName();

    /**
     * Returns the {@link VirtualView} associated with this player.
     * The view is available from the lobby phase onward.
     */
    VirtualView getView();

    /**
     * Activates this player entry for the game phase.
     * For socket connections, starts the reading thread.
     * For RMI connections, installs the disconnect handler.
     * Called exactly once by {@link Game} at game start.
     *
     * @param commandQueue the shared queue into which parsed commands are placed
     * @param onDisconnect callback invoked with the player name on disconnect
     */
    void activate(BlockingQueue<GameCommand> commandQueue,
                  Consumer<String> onDisconnect);
}
