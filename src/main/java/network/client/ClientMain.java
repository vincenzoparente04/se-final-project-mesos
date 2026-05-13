package network.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
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
    private VirtualServer proxy;

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
        router.toSplash();
    }

    @Override
    public void stop() {
        if (router != null && router.getVirtualServer() != null) {
            router.getVirtualServer().close();
        }
    }
    
    public void connect(String transport,String host, int port,String name){

        ConnectionProtocol protocol = ConnectionProtocol.valueOf(transport.toUpperCase());

        new Thread(() -> {
            try {
                VirtualServer connectedProxy = VirtualServerFactory.create(
                        protocol, host, port, name,
                        localState, listener);
                this.proxy = connectedProxy;

                router.setupLocalRouterStatus(name, connectedProxy);

            }catch (Exception ex){
                router.connectionErrorHandling(ex.getMessage());
            }
        }, "connect-" + name).start();
    }
}
