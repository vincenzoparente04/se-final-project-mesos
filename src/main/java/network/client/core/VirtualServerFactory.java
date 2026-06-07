package network.client.core;

import network.client.rmi.RmiVirtualServer;
import network.client.socket.SocketVirtualServer;
import network.server.NetworkUtil;

/**
 * Opens the transport layer for the chosen {@link ConnectionProtocol} and
 * returns an un-named {@link VirtualServer}.
 * <p>
 * The returned proxy still needs to negotiate a player name via
 * {@link VirtualServer#tryRegisterName} (possibly in a retry loop) and then
 * be activated with {@link VirtualServer#start} before any command is sent.
 */
public class VirtualServerFactory {

    private VirtualServerFactory() {}

    /**
     * Establishes the transport-level connection to the server.
     * <p>
     * This method performs blocking network I/O (TCP connect or RMI lookup),
     * so it must <em>not</em> be called on the JavaFX Application Thread.
     *
     * @param transport the protocol to use
     * @param host      server hostname or IP
     * @param port      TCP port (socket) or RMI registry port (RMI)
     * @return a connected, un-named {@link VirtualServer}
     * @throws Exception on connection failure
     */
    public static VirtualServer connect(ConnectionProtocol transport,
                                        String host,
                                        int port) throws Exception {
        return switch (transport) {
            case SOCKET -> new SocketVirtualServer(host, port);
            case RMI    -> {
                // Re-detect the local LAN IP on every connect attempt: if the JVM
                // started with the network down, java.rmi.server.hostname was
                // frozen to 127.0.0.1 in ClientMain.main, which would make the
                // ClientCallbackImpl stub unreachable from the server. Refresh it
                // here so a retry after the network comes up exports stubs with
                // the correct address.
                String localHost = NetworkUtil.detectLocalIPv4();
                System.setProperty("java.rmi.server.hostname", localHost);
                System.out.println("RMI export hostname: " + localHost);
                yield new RmiVirtualServer(host, port);
            }
        };
    }
}
