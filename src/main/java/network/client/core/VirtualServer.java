package network.client.core;

/**
 * Client-side, transport-agnostic proxy of the server: the facade the UI uses to
 * talk to the server. Implementations are {@code SocketVirtualServer} and
 * {@code RmiVirtualServer}. Usage is three-step: connect (via
 * {@code VirtualServerFactory}), {@link #tryRegisterName} (possibly retried), then
 * {@link #start}; afterwards the {@code sendXxx} methods forward user actions.
 */
public interface VirtualServer {

    /**
     * Negotiates the player name with the server. May be called multiple times
     * on the same instance: each call re-uses the already-open transport (TCP
     * socket / RMI registry stub) and only retries the name handshake.
     *
     * @param playerName the requested player name
     * @param localState the local state to be updated from server snapshots
     * @param listener the listener notified of inbound server events
     * @return {@code true} if the server accepted the name and registered the
     *         player; {@code false} if the name was already taken (caller may
     *         retry with a different name).
     */
    boolean tryRegisterName(String playerName,
                            LocalGameState localState,
                            ClientStateListener listener);

    /**
     * Activates the connection after a successful {@link #tryRegisterName}:
     * spawns the reader thread (socket) and starts the heartbeat scheduler.
     * Must be called exactly once, after a {@code true} return from
     * {@link #tryRegisterName}.
     */
    void start();

    /**
     * Sends the local player's color choice.
     * @param color the chosen color name
     */
    void sendChooseColor(String color);

    /**
     * Sends a totem placement on the given offer tile.
     * @param tile the offer-tile letter to place the totem on
     */
    void sendPlaceTotem(char tile);

    /**
     * Sends a draw-card action.
     * @param cardId the id of the card to draw from the visible rows
     */
    void sendDrawCard(int cardId);

    /** Sends an end-of-turn confirmation. */
    void sendEndTurn();

    /**
     * Requests creation of a new lobby.
     * @param maxPlayers the lobby size (2–5)
     */
    void sendCreateLobby(int maxPlayers);

    /**
     * Requests to join an existing lobby.
     * @param lobbyId the id of the lobby to join
     */
    void sendJoinLobby(String lobbyId);

    /** Requests the current list of open lobbies. */
    void sendListLobbies();

    /** Leaves the current lobby or finished game. */
    void sendLeaveCommand();

    /** Closes the connection and releases the transport resources. */
    void close();
}
