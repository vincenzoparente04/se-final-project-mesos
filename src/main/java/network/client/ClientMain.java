package network.client;

import javafx.application.Application;
import javafx.stage.Stage;
import view.SceneRouter;

/**
 * Entry point for the Mesos GUI client.
 * Boots straight into the splash screen — connection parameters are gathered
 * by the nick form, not by command-line arguments.
 */
public class ClientMain extends Application {

    private SceneRouter router;
    private VirtualServer proxy;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        LocalGameState localState = new LocalGameState();
        router = new SceneRouter(primaryStage, localState);

        ClientStateListenerGui listener = new ClientStateListenerGui(router);
        router.setListener(listener);

        primaryStage.setTitle("Mesos");
        router.toSplash();
    }

    @Override
    public void stop() {
        if (router != null && router.virtualServer() != null) {
            router.virtualServer().close();
        }
    }
}
