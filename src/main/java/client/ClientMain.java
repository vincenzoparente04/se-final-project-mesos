package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.List;

/**
 * Entry point for the Mesos client application.
 * <p>
 * <b>Usage</b>:
 * <pre>
 *   ClientMain socket &lt;host&gt; &lt;port&gt;    &lt;playerName&gt;
 *   ClientMain rmi    &lt;host&gt; &lt;rmiPort&gt; &lt;playerName&gt;
 * </pre>
 * The first argument selects the transport:
 * <ul>
 *   <li>{@code socket} — TCP connection via {@link client.socket.SocketVirtualServer}</li>
 *   <li>{@code rmi}    — RMI connection via {@link client.rmi.RmiVirtualServer}</li>
 * </ul>
 * For backwards compatibility, if the first argument is not a transport keyword
 * (i.e. it looks like a hostname) the client defaults to {@link ConnectionProtocol#SOCKET}
 * and treats the arguments as {@code <host> <port> <playerName>}.
 * <p>
 * The network connection is established on a background thread so the JavaFX
 * stage opens immediately, independently of server availability.
 */
public class ClientMain extends Application {

    private static ConnectionProtocol transport;
    private static String host;
    private static int port;
    private static String playerName;

    // ─────────────────────────────────────────────────────────
    // Main entry — parses args and launches JavaFX
    // ─────────────────────────────────────────────────────────

    public static void main(String[] args) {
        if (args.length < 3) {
            printUsage();
            return;
        }

        String firstArg = args[0].toLowerCase();
        if (firstArg.equals("socket") || firstArg.equals("rmi")) {
            // New-style: <transport> <host> <port> <playerName>
            if (args.length < 4) { printUsage(); return; }
            transport  = ConnectionProtocol.from(args[0]);
            host       = args[1];
            port       = Integer.parseInt(args[2]);
            playerName = args[3];
        } else {
            // Legacy / default: <host> <port> <playerName>  (socket)
            transport  = ConnectionProtocol.SOCKET;
            host       = args[0];
            port       = Integer.parseInt(args[1]);
            playerName = args[2];
        }

        launch(args);
    }

    // ─────────────────────────────────────────────────────────
    // JavaFX start
    // ─────────────────────────────────────────────────────────

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Mesos — " + playerName + " [connecting via " + transport + "...]");
        primaryStage.show();

        LocalGameState localState = new LocalGameState();
        ClientStateListener listener = new ClientStateListenerGui(primaryStage, playerName);

        // Connect on a background thread — never block the Application Thread
        // with network I/O.
        Thread connectThread = new Thread(
                () -> connect(transport, host, port, playerName, localState, listener, primaryStage),
                "connect-" + playerName);
        connectThread.setDaemon(true);
        connectThread.start();
    }

    // ─────────────────────────────────────────────────────────
    // Connection logic (runs on background thread)
    // ─────────────────────────────────────────────────────────

    private static void connect(ConnectionProtocol transport,
                                String host, int port, String playerName,
                                LocalGameState localState,
                                ClientStateListener listener,
                                Stage primaryStage) {
        try {
            VirtualServer proxy = VirtualServerFactory.create(
                    transport, host, port, playerName, localState, listener);

            Platform.runLater(() ->
                    primaryStage.setTitle("Mesos — " + playerName));

            // TODO: attach proxy and localState to the real View scene graph

        } catch (Exception e) {
            Platform.runLater(() -> {
                primaryStage.setTitle("Mesos — connection failed");
                System.err.println("Could not connect to server: " + e.getMessage());
            });
        }
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  ClientMain socket <host> <port>    <playerName>");
        System.err.println("  ClientMain rmi    <host> <rmiPort> <playerName>");
    }
}
