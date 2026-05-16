package network.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import view.LobbyViewController;

/**
 * Entry point for the Mesos GUI client.
 *
 * Usage:
 *   ClientMain socket <host> <port> <playerName>
 *   ClientMain rmi <host> <rmiPort> <playerName>
 *
 * The lobby scene is shown immediately; the network connection is established
 * on a background thread so the stage opens without blocking.
 */
public class ClientMain extends Application {

    private static ConnectionProtocol transport;
    private static String host;
    private static int port;
    private static String playerName;
    private VirtualServer proxy;


    // Main parse args, launch JavaFX
    public static void main(String[] args) {
        if (args.length < 3) {
            printUsage();
            return;
        }

        String first = args[0].toLowerCase();
        if (first.equals("socket") || first.equals("rmi")) {
            if (args.length < 4) { printUsage(); return; }
            transport  = ConnectionProtocol.from(args[0]);
            host       = args[1];
            port       = Integer.parseInt(args[2]);
            playerName = args[3];
        } else {
            //<host> <port> <playerName>
            transport  = ConnectionProtocol.SOCKET;
            host       = args[0];
            port       = Integer.parseInt(args[1]);
            playerName = args[2];
        }

        launch(args);
    }

    // JavaFX start

    /**
     * Initializes and displays the lobby scene immediately.
     * <p>
     * This method is called by JavaFX after the application is launched. It performs the following:
     * <ul>
     *   <li>Loads the lobby-view.fxml and retrieves the LobbyViewController controller.</li>
     *   <li>Initializes the lobby controller with the player name.</li>
     *   <li>Creates a ClientStateListenerGui to wire network callbacks to UI updates.</li>
     *   <li>Creates a LocalGameState to hold the current game state snapshot.</li>
     *   <li>Displays the lobby scene immediately (non-blocking).</li>
     *   <li>Spawns a background thread to establish the network connection in parallel.</li>
     * </ul>
     * <p>
     * The UI remains responsive while the connection is being established on the background thread.
     *
     * @param primaryStage the main Stage provided by JavaFX
     * @throws Exception if the FXML resource cannot be loaded
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load lobby scene
        FXMLLoader loader = new FXMLLoader(ClientMain.class.getResource("/org/example/mesos/lobby-view.fxml"));
        Parent root = loader.load();
        LobbyViewController lobbyCtrl = loader.getController();
        lobbyCtrl.init(playerName);

        // Create listener (wires lobby controller to network callbacks)
        LocalGameState localState = new LocalGameState();
        ClientStateListenerGui listener =
                new ClientStateListenerGui(primaryStage, playerName, lobbyCtrl);

        // Show the lobby scene immediately
        primaryStage.setTitle("Mesos — " + playerName + "  [connecting...]");
        primaryStage.setScene(new Scene(root, 700, 520));
        primaryStage.show();

        // Connect on a background thread — never block the Application Thread
        Thread connectThread = new Thread(
                () -> connect(listener, localState, primaryStage),
                "connect-" + playerName);
        connectThread.setDaemon(true);
        connectThread.start();
    }

    // Connection logic (runs on background thread)

    /**
     * Establishes the network connection to the server on a background thread.
     * <p>
     * This method is invoked asynchronously from {@link #start(Stage)} to avoid blocking the
     * JavaFX Application Thread. It performs the following steps:
     * <ul>
     *   <li>Creates a VirtualServer proxy (Socket or RMI) based on connection parameters.</li>
     *   <li>Propagates the proxy to the ClientStateListenerGui for UI wiring.</li>
     *   <li>Sends an initial sendListLobbies() request to populate the lobby list.</li>
     *   <li>Updates the stage title to reflect successful connection.</li>
     * </ul>
     * <p>
     * If the connection fails, an error message is printed and the stage title reflects the failure.
     * <p>
     * <b>Threading:</b> This method runs on a daemon background thread (connect-{playerName}).
     * All UI updates are marshalled back to the JavaFX Application Thread via {@link Platform#runLater(Runnable)}.
     *
     * @param listener the ClientStateListenerGui that routes network callbacks to UI updates
     * @param localState the shared LocalGameState instance to receive game state snapshots
     * @param primaryStage the primary Stage to update with connection status
     */
    private void connect(ClientStateListenerGui listener, LocalGameState localState, Stage primaryStage) {
        try {
            VirtualServer proxy = VirtualServerFactory.connect(transport, host, port);

            if (!proxy.tryRegisterName(playerName, localState, listener)) {
                proxy.close();
                Platform.runLater(() ->
                        primaryStage.setTitle("Mesos — name '" + playerName + "' already taken"));
                System.err.println("Could not connect: name '" + playerName + "' is already taken on the server.");
                return;
            }
            proxy.start();
            this.proxy = proxy;

            // Propagate the VirtualServer proxy to the listener and UI controller
            listener.setVirtualServer(proxy);

            // Ask the server for the current lobby list right away
            proxy.sendListLobbies();

            Platform.runLater(() -> primaryStage.setTitle("Mesos — " + playerName));

        } catch (Exception e) {
            Platform.runLater(() -> {
                primaryStage.setTitle("Mesos — connection failed");
                System.err.println("Could not connect to server: " + e.getMessage());
            });
        }
    }

    //method to close the RMI connection when the application is closed
    @Override
    public void stop() {
        if (proxy != null) proxy.close();
    }

    // Helpers

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  ClientMain socket <host> <port>    <playerName>");
        System.err.println("  ClientMain rmi    <host> <rmiPort> <playerName>");
    }
}
