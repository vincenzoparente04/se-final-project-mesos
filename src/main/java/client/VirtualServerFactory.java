package client;

import client.rmi.RmiVirtualServer;
import client.socket.SocketVirtualServer;

/**
 * Creates a {@link VirtualServer} for the chosen {@link ConnectionProtocol}.
 * <p>
 * {@link ClientMain} calls this factory so it never needs to import
 * a concrete transport class directly — only the {@link ConnectionProtocol} enum
 * and this factory cross the boundary.
 */
public class VirtualServerFactory {

    private VirtualServerFactory() {}

    /**
     * Creates and connects a {@link VirtualServer} for the given transport.
     * <p>
     * This method performs blocking network I/O (TCP connect or RMI lookup),
     * so it must <em>not</em> be called on the JavaFX Application Thread.
     *
     * @param transport  the protocol to use
     * @param host       server hostname or IP
     * @param port       TCP port (socket) or RMI registry port (RMI)
     * @param playerName display name for this player
     * @param localState client-side game-state cache to update on each snapshot
     * @param listener   callbacks invoked when the server sends events
     * @return a connected, ready-to-use {@link VirtualServer}
     * @throws Exception on connection failure
     */
    public static VirtualServer create(ConnectionProtocol transport,
                                     String host,
                                     int port,
                                     String playerName,
                                     LocalGameState localState,
                                     ClientStateListener listener) throws Exception {
        return switch (transport) {
            case SOCKET -> new SocketVirtualServer(host, port, playerName, localState, listener);
            case RMI    -> new RmiVirtualServer(host, port, playerName, localState, listener);
        };
    }
}
