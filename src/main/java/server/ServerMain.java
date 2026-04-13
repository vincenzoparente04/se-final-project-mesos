package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Entry point for the Mesos game server.
 * <p>
 * Usage: {@code java server.ServerMain <port> <numberOfPlayers>}
 * <p>
 * Accepts connections in a loop and delegates each socket to
 * {@link LobbyManager}.  The server is passive: it only reacts to
 * client messages — it never drives the game by itself.
 */
public class ServerMain {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ServerMain <port> <numberOfPlayers>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int numberOfPlayers = Integer.parseInt(args[1]);

        LobbyManager lobby = new LobbyManager(numberOfPlayers);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Mesos server listening on port " + port
                    + " — waiting for " + numberOfPlayers + " players.");

            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("New connection from " + client.getInetAddress());
                new Thread(() -> lobby.addClient(client)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
