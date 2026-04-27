package server.core;

import server.rmi.GameServerRemoteImpl;
import shared.command.GameCommand;
import shared.rmi.GameServerRemote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Entry point for the Mesos game server.
 * <p>
 * <p>
 * Starts two transport listeners that share the same {@link LobbyManager}
 * and command queue:
 * <ul>
 *   <li><b>TCP socket</b> on {@code port} — classic text-protocol clients.</li>
 *   <li><b>RMI registry</b> on {@code rmiPort} (default 1099) — RMI clients
 *       look up {@code "MesosGameServer"} and call
 *       {@link shared.rmi.GameServerRemote#join} and
 *       {@link shared.rmi.GameServerRemote#submitCommand}.</li>
 * </ul>
 * Both transports contribute players to the same game: the server is
 * completely agnostic about which protocol each player uses.
 */
public class ServerMain {

    private static final String RMI_SERVICE_NAME = "MesosGameServer";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: ServerMain <port> [rmiPort]");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int rmiPort = args.length >= 2 ? Integer.parseInt(args[1]) : 1099;

        LobbyManager lobby = new LobbyManager();

        startRmiRegistry(lobby, rmiPort);
        startSocketAcceptor(lobby, port);
    }

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

    private static void startSocketAcceptor(LobbyManager lobby, int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Socket server on port " + port + " — waiting for connections.");

            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Socket connection from " + client.getInetAddress());
                new Thread(() -> lobby.openSocketConnection(client)).start();
            }
        } catch (IOException e) {
            System.err.println("Socket server error: " + e.getMessage());
        }
    }
}
