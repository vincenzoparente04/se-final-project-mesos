package client;

/**
 * Selects the network transport protocol for the client connection.
 * Passed to {@link VirtualServerFactory} to create the appropriate
 * {@link VirtualServer} implementation.
 */
public enum ConnectionProtocol {

    /** TCP socket with text protocol ({@link client.socket.SocketServerProxy}). */
    SOCKET,

    /** Java RMI with callback stubs ({@link client.rmi.RmiServerProxy}). */
    RMI;

    /**
     * Parses a transport name case-insensitively.
     *
     * @param value {@code "socket"} or {@code "rmi"}
     * @return the matching {@link ConnectionProtocol}
     * @throws IllegalArgumentException if the value is not recognised
     */
    public static ConnectionProtocol from(String value) {
        return switch (value.toLowerCase()) {
            case "socket" -> SOCKET;
            case "rmi"    -> RMI;
            default       -> throw new IllegalArgumentException(
                    "Unknown transport '" + value + "'. Use 'socket' or 'rmi'.");
        };
    }
}
