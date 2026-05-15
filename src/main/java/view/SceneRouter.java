package view;

import network.client.ClientMain;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import network.client.ClientSession;
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

    private ClientSession session;

    private StackPane currentRoot;

    private NickViewController nickViewController;
    private SceneController currentViewController;

    public SceneRouter(Stage stage, LocalGameState localState, ClientMain main) {
        this.clientMain = main;
        this.stage = stage;
        this.localState = localState;
    }

    // Bindings set as the user progresses through screens ────────────────

    public void setListener(ClientStateListener l) { this.listener = l; }
    public ClientStateListener listener()          { return listener; }
    public VirtualServer getVirtualServer()        { return session != null ? session.virtualServer() : null; }
    public String playerName()                     { return session != null ? session.playerName() : null; }
    public LocalGameState localState()             { return localState; }
    public SceneController currentController()     { return currentViewController; }
    public StackPane currentRoot()                 { return currentRoot; }
    public Stage stage()                           { return stage; }

    // Navigation ─────────────────────────────────────────────────────────


    public void toSplash() {
        load("/org/example/mesos/splash-view.fxml");
    }

    public void toNick() {
        load("/org/example/mesos/nick-view.fxml");
    }

    public void toLobby() {
        load("/org/example/mesos/lobby-view.fxml");
        if (getVirtualServer() != null) getVirtualServer().sendListLobbies();
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
        load("/org/example/mesos/board/board-view.fxml");
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
            loader.load();
            SceneController ctrl = loader.getController();
            this.currentViewController = ctrl;
            StackPane root = ctrl.root();
            this.currentRoot = root;

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

    public void setupSession(String name, VirtualServer vs) {
        this.session = new ClientSession(name, vs);
        Platform.runLater(this::toLobby);
    }

    public void connectionErrorHandling(String message) {
        nickViewController.onConnectionError(message);
    }
}
