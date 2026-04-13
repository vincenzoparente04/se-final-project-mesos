package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Entry point for the Mesos client application.
 * <p>
 * Usage: {@code java client.ClientMain <host> <port> <playerName>}
 * <p>
 * Creates the {@link LocalGameState}, connects the {@link VirtualServer},
 * and wires up the {@link ClientController}.
 * The View (JavaFX Scene) will be set up here once the view package is implemented.
 */
public class ClientMain extends Application {

    private static String host;
    private static int port;
    private static String playerName;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: ClientMain <host> <port> <playerName>");
            return;
        }
        host = args[0];
        port = Integer.parseInt(args[1]);
        playerName = args[2];
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        LocalGameState localState = new LocalGameState();

        // Placeholder listener — will be replaced by the real View once implemented.
        // All callbacks are dispatched onto the JavaFX Application Thread via Platform.runLater.
        ClientStateListener listener = new ClientStateListener() {
            @Override
            public void onGameStateUpdated(LocalGameState state) {
                Platform.runLater(() -> System.out.println("[STATE] phase=" + state.getPhase()
                        + " round=" + state.getCurrentRound()
                        + " currentPlayer=" + state.getCurrentPlayerName()));
            }

            @Override
            public void onWaiting(String rawWaitingMessage) {
                Platform.runLater(() -> System.out.println("[WAITING] " + rawWaitingMessage));
            }

            @Override
            public void onError(String message) {
                Platform.runLater(() -> System.out.println("[ERROR] " + message));
            }
        };

        try {
            VirtualServer virtualServer = new VirtualServer(host, port, playerName, localState, listener);
            @SuppressWarnings("unused")
            ClientController controller = new ClientController(virtualServer);

            primaryStage.setTitle("Mesos — " + playerName);
            primaryStage.show();

            // TODO: attach controller and localState to the real View scene graph
        } catch (Exception e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        }
    }
}
