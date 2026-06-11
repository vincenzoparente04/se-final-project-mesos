package network.client.core;

import javafx.application.Application;
import javafx.stage.Stage;
import network.server.NetworkUtil;
import view.MusicManager;
import view.SceneRouter;

/**
 * Entry point for the Mesos GUI client.
 * Boots straight into the splash screen, and the SceneRouter handles all subsequent navigation. The main responsibilities of this class are:
 * <ul>
 *   <li>Setting the RMI export hostname to the local LAN IP address to ensure callbacks are reachable by the server.</li>
 *   <li>Initializing the SceneRouter and ClientStateListenerGui to bridge network events to the UI.</li>
 *   <li>Providing methods to establish the server connection and register the player's name, which are called by the SceneRouter during the connection flow.</li>
 *   <li>Cleaning up resources on application shutdown, such as stopping music and closing the virtual server connection.</li>
 * </ul>
 * <p>
 * The actual game logic and UI updates are handled by the SceneRouter and the controllers it manages, while this class serves as the bootstrap and high-level coordinator for the client application.
 */
public class ClientMain extends Application {

    /** Connected proxy to the server; set after a successful connect. */
    private VirtualServer virtualServer;
    /** Local mirror of the game state, updated from server snapshots. */
    private LocalGameState localState;
    /** Bridges inbound server events to the JavaFX UI. */
    private ClientStateListenerGui listener;
    /** Navigation hub for the GUI scenes. */
    private SceneRouter router;

    /**
     * GUI client entry point: advertises a LAN-reachable RMI hostname, then launches
     * the JavaFX application.
     *
     * @param args JavaFX application arguments
     */
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
     *   <li>Creates a LocalGameState to hold the current game state snapshot.</li>
     *   <li>Creates the SceneRouter that is the navigation hub of the GUI.</li>
     *   <li>Creates a ClientStateListenerGui to wire network callbacks to UI updates.</li>
     *   <li>Asks the router to display the splash scene immediately.</li>
     * </ul>
     * <p>
     * @param primaryStage the main Stage provided by JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        localState = new LocalGameState();
        router = new SceneRouter(primaryStage, localState, this);

        listener = new ClientStateListenerGui(router);

        primaryStage.setTitle("Mesos");
        router.toSplash();
    }

    /**
     *  Cleans up resources when the application is closed. This includes:
     * <ul>
     *   <li>Stopping any music playback.</li>
     *   <li>Closing the virtual server connection if it exists.</li>
     * </ul>
     */
    @Override
    public void stop() {
        MusicManager.getInstance().stop();
        if (router != null && router.getVirtualServer() != null) {
            router.getVirtualServer().close();
        }
    }

    /**
     * Establishes the connection to the server.
     * @param transport the protocol to use ("socket" or "rmi")
     * @param host server hostname or IP
     * @param port server port
     */
    public void connect(String transport, String host, int port) {
        ConnectionProtocol protocol = ConnectionProtocol.valueOf(transport.toUpperCase());

            try {
                virtualServer = VirtualServerFactory.connect(protocol, host, port);

                router.connectionEstablished();
            } catch (Exception ex) {
                router.connectionErrorHandling(ex.getMessage());
            }
    }

    /**
     * Attempts to register the player's name with the server. If successful, sets up the client session and starts receiving updates.
     * If unsuccessful calls router.nickRejected() to show an error message and let the user retry with a different name.
     * @param name player's name
     */
    public void setName(String name){

        if(virtualServer.tryRegisterName(name, localState, listener)){
            router.setupSession(name, virtualServer);
            virtualServer.start();
        } else {
            router.nickRejected();
        }
    }
}
