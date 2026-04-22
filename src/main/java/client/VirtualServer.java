package client;

/**
 * Protocol-agnostic API for sending game commands to the server.
 * <p>
 * {@link ClientController} depends on this interface, not on a concrete
 * transport.  Concrete implementations:
 * <ul>
 *   <li>{@link client.socket.SocketVirtualServer} — TCP socket transport</li>
 *   <li>{@link client.rmi.RmiVirtualServer} — RMI transport</li>
 * </ul>
 */
public interface VirtualServer {

    /** Sends a totem-colour choice to the server. */
    void sendChooseColor(String color);

    /** Sends a totem placement on the given offer-tile letter. */
    void sendPlaceTotem(char tile);

    /** Sends a card-draw request for the given card ID. */
    void sendDrawCard(int cardId);

    /** Signals that the current player's turn is over. */
    void sendEndTurn();

    /** Closes the underlying connection. */
    void close();
}
