package view;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import network.client.ClientMain;
import network.client.ClientStateListener;
import network.client.LocalGameState;
import network.client.VirtualServer;
import shared.dto.LobbyDto;
import shared.dto.PlayerDto;

import java.io.IOException;
import java.util.List;

/**
 * Single navigation hub for the whole client UI.
 * Each screen calls back into the router to move forward (or back),
 * and the network listener uses it to know which controller is on screen
 * and to overlay toasts on top of the current root.
 */
public class SceneRouter {

    private final ClientMain clientMain;
    private final Stage stage;
    private final LocalGameState localState;
    private ClientStateListener listener;

    private VirtualServer virtualServer;
    private String playerName;

    private Object currentController;
    private StackPane currentRoot;

    private NickViewController nickViewController;
    private ViewController currentViewController;

    public SceneRouter(Stage stage, LocalGameState localState, ClientMain main) {
        this.clientMain = main;
        this.stage = stage;
        this.localState = localState;
    }

    // Bindings set as the user progresses through screens ────────────────

    public void setListener(ClientStateListener l) { this.listener = l; }
    public ClientStateListener listener()          { return listener; }
    public void setVirtualServer(VirtualServer vs) { this.virtualServer = vs; }
    public VirtualServer getVirtualServer()           { return virtualServer; }
    public void setPlayerName(String name)         { this.playerName = name; }
    public String playerName()                     { return playerName; }
    public LocalGameState localState()             { return localState; }
    public ViewController currentController() { return currentViewController; }
    public StackPane currentRoot()                 { return currentRoot; }
    public Stage stage()                           { return stage; }

    // Navigation ─────────────────────────────────────────────────────────

    public void exitApplication() {
        Platform.exit();
        System.exit(0);
    }

    public void toSplash() {
        load("/org/example/mesos/splash-view.fxml");
    }

    public void toNick() {
        load("/org/example/mesos/nick-view.fxml");
    }

    public void toLobby() {
        load("/org/example/mesos/lobby-view.fxml");
        if (virtualServer != null) virtualServer.sendListLobbies();
    }

    public void toWaiting(LobbyDto lobby) {
        load("/org/example/mesos/waiting-view.fxml");
        //TODO: review how to do this setLobby
        currentViewController.setLobby(lobby);
    }

    public void toTotemPick() {
        load("/org/example/mesos/totem-pick-view.fxml");
    }

    public void toBoard() {
        load("/org/example/mesos/board-view.fxml");
    }

    public void toWinner(List<PlayerDto> players, List<String> winners) {
        load("/org/example/mesos/winner-view.fxml");
        //TODO: remove this terrible function
        currentViewController.showWinners(players, winners);
    }

    // FXML loading ──────────────────────────────────────────────────────

    private void load(String fxmlResource) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlResource));
            Parent root = loader.load();
            ViewController ctrl = loader.getController();
            this.currentViewController = ctrl;

            //TODO: remove casting
            this.currentRoot = (StackPane) root;

            currentViewController.bind(this);

            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root, 1280, 800);
                String css = getClass().getResource("/styles/mesos.css").toExternalForm();
                scene.getStylesheets().add(css);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load " + fxmlResource + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Connection Handling ──────────────────────────────────────────

    public void connect(String transport, String host, int port, String name, NickViewController controller) {
        clientMain.connect(transport, host, port, name);
        this.nickViewController = controller;
    }
    
    public void setupLocalRouterStatus(String name, VirtualServer vs) {
        setPlayerName(name);
        setVirtualServer(vs);
        Platform.runLater(() -> {
            toLobby();
        });
    }

    public void connectionErrorHandling(String message) {
        nickViewController.onConnectionError(message);
    }
    
    // Update state ──────────────────────────────────────────
    
    public void update(LocalGameState state) {
        currentViewController.update(state);
    }

}
