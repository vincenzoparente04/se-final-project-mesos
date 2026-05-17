package network.client.core;

import javafx.application.Application;
import javafx.application.Platform; //TODO: remove r 4,5,6,7
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import network.NetworkUtil;
import view.LobbyViewController;
import view.MusicManager;
import view.SceneRouter;

/**
 * Entry point for the Mesos GUI client.
 *Boots straight into the splash screen — connection parameters are gathered
 *by the nick form, not by command-line arguments.
 */
public class ClientMain extends Application {

    private static ConnectionProtocol transport;
    private static String host;
    private static int port;
    private static String playerName; //CI SERVE O NON CISERVE ?
    private VirtualServer virtualServer;
    private LocalGameState localState;
    private ClientStateListenerGui listener;
    private SceneRouter router;

    public static void main(String[] args) {
        // Advertise the local LAN IP so the RMI callback stub exported by
        // RmiVirtualServer carries a reachable address, not 127.0.0.1.
        String localHost = NetworkUtil.detectLocalIPv4();
        System.setProperty("java.rmi.server.hostname", localHost);
        System.out.println("RMI export hostname: " + localHost);

        /*
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
        }*/

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
    public void start(Stage primaryStage) {
        localState = new LocalGameState();
        router = new SceneRouter(primaryStage, localState, this);

        listener = new ClientStateListenerGui(router);
        router.setListener(listener);

        primaryStage.setTitle("Mesos");
        MusicManager.getInstance().play("music/background.mp3");
        router.toSplash();
    }

    @Override
    public void stop() {
        MusicManager.getInstance().stop();
        if (router != null && router.getVirtualServer() != null) {
            router.getVirtualServer().close();
        }
    }

    public void connect(String transport, String host, int port) {
        ConnectionProtocol protocol = ConnectionProtocol.valueOf(transport.toUpperCase());

            try {
                virtualServer = VirtualServerFactory.connect(
                        protocol, host, port);

                router.connectionEstablished();
            } catch (Exception ex) {
                router.connectionErrorHandling(ex.getMessage());
            }
    }

    public void setName(String name){

        if(virtualServer.tryRegisterName(name, localState, listener)){
            router.setupSession(name, virtualServer);
            virtualServer.start();
        } else {
            router.nickRejected();
        }
    }
}
