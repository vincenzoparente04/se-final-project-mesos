package network.client.core;

public interface VirtualServer {

    /**
     * Negotiates the player name with the server. May be called multiple times
     * on the same instance: each call re-uses the already-open transport (TCP
     * socket / RMI registry stub) and only retries the name handshake.
     *
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

    void sendChooseColor(String color);

    void sendPlaceTotem(char tile);

    void sendDrawCard(int cardId);

    void sendEndTurn();

    void sendCreateLobby(int maxPlayers);

    void sendJoinLobby(String lobbyId);

    void sendListLobbies();

    void sendLeaveCommand();

    void close();
}
