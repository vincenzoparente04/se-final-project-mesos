package network.client;

import javafx.application.Application;
import javafx.stage.Stage;
import view.MusicManager;
import view.SceneRouter;

/**
 * Entry point for the Mesos GUI client.
 * Boots straight into the splash screen — connection parameters are gathered
 * by the nick form, not by command-line arguments.
 */
public class ClientMain extends Application {

    private LocalGameState localState;
    private ClientStateListenerGui listener;
    private SceneRouter router;
    private VirtualServer virtualServer;

    public static void main(String[] args) {
        launch(args);
    }

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
