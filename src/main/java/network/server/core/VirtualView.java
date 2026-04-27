package server.core;

import shared.dto.GameStateDto;

/**
 * Protocol-agnostic view of one connected player.
 * <p>
 * {@link Game} holds a list of these and never knows whether the
 * underlying transport is a TCP socket or RMI.  Concrete implementations
 * ({@link server.socket.SocketVirtualView}, {@link server.rmi.RmiVirtualView})
 * handle the transport-specific details of sending and closing.
 */
public interface VirtualView {

    /**
     * Sends a complete game-state snapshot to the client.
     * Implementations must not block the caller when the transport is slow.
     */
    void sendState(GameStateDto dto);

    /**
     * Sends an error message to the client (e.g. rejected command,
     * player disconnected).
     */
    void sendError(String message);

    /**
     * Sends a lobby waiting notification.
     * Called before the game starts while waiting for more players.
     *
     * @param current  number of players currently in the lobby
     * @param expected total players required to start
     */
    void sendWaiting(int current, int expected);

    /** Returns the display name of the player this view represents. */
    String getPlayerName();

    /** Closes the underlying transport channel. Idempotent. */
    void close();
}
