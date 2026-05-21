package network.client.core;

import javafx.application.Application;
import javafx.stage.Stage;
import network.server.NetworkUtil;
import view.MusicManager;
import view.SceneRouter;

/**
 * Entry point for the Mesos GUI client.
 *Boots straight into the splash screen — connection parameters are gathered
 *by the nick form, not by command-line arguments.
 */
public class ClientMain extends Application {

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
        MusicManager.getInstance().playRandom("music/scaricamusicayoutube");
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
                virtualServer = VirtualServerFactory.connect(protocol, host, port);

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
