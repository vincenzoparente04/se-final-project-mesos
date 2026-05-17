package network.server.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import network.server.rmi.GameServerRemote;
import network.server.rmi.GameServerRemoteImpl;

/**
 * Entry point of the Mesos game server.
 * <p>
 * Boots two transport listeners that share a single {@link LobbyManager}:
 * an RMI registry (for clients connecting via Java RMI) and a TCP socket
 * acceptor (for clients connecting via the textual socket protocol).
 * The two listeners are independent: a failure in one does not prevent
 * the other from serving clients.
 * <p>
 * Uses fixed ports:
 * - Socket server: 9999
 * - RMI registry: 1099 (standard Java RMI port)
 *
 * @implNote The {@code main} method does not return: after starting the RMI
 *           registry, control enters the socket acceptor's blocking
 *           {@code accept()} loop and remains there for the lifetime of the
 *           server process.
 */
public class ServerMain {

    private static final String RMI_SERVICE_NAME = "MesosGameServer";
    private static final int SOCKET_PORT = 9999;
    private static final int RMI_PORT = 1099;

    public static void main(String[] args) {

        LobbyManager lobby = new LobbyManager();

        // TODO: vedi se necessario
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down server…");
            lobby.shutdown();
        }, "server-shutdown"));

        startRmiRegistry(lobby);
        startSocketAcceptor(lobby);
    }


    /**
     * Creates an RMI registry on the given port and binds the
     * {@link GameServerRemoteImpl} stub under {@value #RMI_SERVICE_NAME}.
     * If registry creation fails the error is logged and the server
     * continues to start the socket acceptor — RMI clients will be unable
     * to connect, but socket clients will work normally.
     */
    private static void startRmiRegistry(LobbyManager lobby) {
        try {
            Registry registry = LocateRegistry.createRegistry(ServerMain.RMI_PORT);
            GameServerRemote stub = new GameServerRemoteImpl(lobby);
            registry.rebind(RMI_SERVICE_NAME, stub);
            System.out.println("RMI registry on port " + ServerMain.RMI_PORT + " — service name: " + RMI_SERVICE_NAME);
        } catch (Exception e) {
            System.err.println("Failed to start RMI registry: " + e.getMessage());
        }
    }

    /**
     * Opens the TCP listening socket and enters the accept loop. For each
     * incoming connection spawns a thread that delegates the full handshake + client
     * setup to {@link LobbyManager#openSocketConnection(Socket)}.
     * <p>
     * This method blocks for the lifetime of the server: it returns only if
     * the listening socket itself is closed or fails.
     */
    private static void startSocketAcceptor(LobbyManager lobby) {
        try (ServerSocket serverSocket = new ServerSocket(ServerMain.SOCKET_PORT)) {
            System.out.println("Socket server on port " + ServerMain.SOCKET_PORT + " — waiting for connections.");


            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Socket connection from " + client.getInetAddress());
                Thread t = new Thread(() -> lobby.openSocketConnection(client),"socket-handshake-" + client.getPort());
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Socket server error: " + e.getMessage());
        }
    }
}
