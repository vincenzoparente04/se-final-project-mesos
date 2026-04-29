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
 * Invocation: {@code ServerMain <port> [rmiPort]}. The first argument is
 * the TCP port for socket clients; the second (optional, defaults to 1099)
 * is the port on which the RMI registry is created.
 *
 * @implNote The {@code main} method does not return: after starting the RMI
 *           registry, control enters the socket acceptor's blocking
 *           {@code accept()} loop and remains there for the lifetime of the
 *           server process.
 */
public class ServerMain {

    private static final String RMI_SERVICE_NAME = "MesosGameServer";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: ServerMain <port> [rmiPort]");
            return;
        }
        /*
        int port = Integer.parseInt(args[0]);
        int rmiPort = args.length >= 2 ? Integer.parseInt(args[1]) : 1099;
        */
        int port;
        int rmiPort;
        try {
            port = Integer.parseInt(args[0]);
            rmiPort = args.length >= 2 ? Integer.parseInt(args[1]) : 1099;
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + e.getMessage());
            return;
        }

        LobbyManager lobby = new LobbyManager();

        startRmiRegistry(lobby, rmiPort);
        startSocketAcceptor(lobby, port);
    }


    /**
     * Creates an RMI registry on the given port and binds the
     * {@link GameServerRemoteImpl} stub under {@value #RMI_SERVICE_NAME}.
     * If registry creation fails the error is logged and the server
     * continues to start the socket acceptor — RMI clients will be unable
     * to connect, but socket clients will work normally.
     */
    private static void startRmiRegistry(LobbyManager lobby, int rmiPort) {
        try {
            Registry registry = LocateRegistry.createRegistry(rmiPort);
            GameServerRemote stub = new GameServerRemoteImpl(lobby);
            registry.rebind(RMI_SERVICE_NAME, stub);
            System.out.println("RMI registry on port " + rmiPort + " — service name: " + RMI_SERVICE_NAME);
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
    private static void startSocketAcceptor(LobbyManager lobby, int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Socket server on port " + port + " — waiting for connections.");


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
