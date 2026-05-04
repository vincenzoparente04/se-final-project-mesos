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
    private static void connect(ClientStateListenerGui listener, LocalGameState localState, Stage primaryStage) {
        try {
            VirtualServer proxy = VirtualServerFactory.create(transport, host, port, playerName, localState, listener);

            ClientController controller = new ClientController(proxy);

            // Propagate the controller to the lobby buttons
            listener.setClientController(controller);

            // Ask the server for the current lobby list right away
            controller.onListLobbies();

            Platform.runLater(() -> primaryStage.setTitle("Mesos — " + playerName));

        } catch (Exception e) {
            Platform.runLater(() -> {
                primaryStage.setTitle("Mesos — connection failed");
                System.err.println("Could not connect to server: " + e.getMessage());
            });
        }
    }

    // Helpers

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  ClientMain socket <host> <port>    <playerName>");
        System.err.println("  ClientMain rmi    <host> <rmiPort> <playerName>");
    }
}
