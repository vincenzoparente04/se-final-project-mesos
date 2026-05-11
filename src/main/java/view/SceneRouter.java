package view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
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

    private final Stage stage;
    private final LocalGameState localState;
    private ClientStateListener listener;

    private VirtualServer virtualServer;
    private String playerName;

    private Object currentController;
    private StackPane currentRoot;

    public SceneRouter(Stage stage, LocalGameState localState) {
        this.stage = stage;
        this.localState = localState;
    }

    // ── Bindings set as the user progresses through screens ────────────────

    public void setListener(ClientStateListener l) { this.listener = l; }
    public ClientStateListener listener()          { return listener; }
    public void setVirtualServer(VirtualServer vs) { this.virtualServer = vs; }
    public VirtualServer virtualServer()           { return virtualServer; }
    public void setPlayerName(String name)         { this.playerName = name; }
    public String playerName()                     { return playerName; }
    public LocalGameState localState()             { return localState; }
    public Object currentController()              { return currentController; }
    public StackPane currentRoot()                 { return currentRoot; }
    public Stage stage()                           { return stage; }

    // ── Navigation ─────────────────────────────────────────────────────────

    public void toSplash() {
        load("/org/example/mesos/splash-view.fxml", ctrl -> ((SplashViewController) ctrl).bind(this));
    }

    public void toNick() {
        load("/org/example/mesos/nick-view.fxml", ctrl -> ((NickViewController) ctrl).bind(this));
    }

    public void toLobby() {
        load("/org/example/mesos/lobby-view.fxml", ctrl -> ((LobbyViewController) ctrl).bind(this));
        if (virtualServer != null) virtualServer.sendListLobbies();
    }

    public void toWaiting(LobbyDto lobby) {
        load("/org/example/mesos/waiting-view.fxml",
                ctrl -> ((WaitingViewController) ctrl).bind(this, lobby));
    }

    public void toBoard() {
        load("/org/example/mesos/board-view.fxml",
                ctrl -> ((BoardViewController) ctrl).bind(this));
    }

    public void toWinner(List<PlayerDto> players, List<String> winners) {
        load("/org/example/mesos/winner-view.fxml",
                ctrl -> ((WinnerViewController) ctrl).bind(this, players, winners));
    }

    // ── FXML loading ──────────────────────────────────────────────────────

    private void load(String fxmlResource, java.util.function.Consumer<Object> binder) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlResource));
            Parent root = loader.load();
            Object ctrl = loader.getController();
            this.currentController = ctrl;
            if (root instanceof StackPane sp) {
                this.currentRoot = sp;
            } else {
                StackPane wrapper = new StackPane(root);
                this.currentRoot = wrapper;
                root = wrapper;
            }
            binder.accept(ctrl);

            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root, 1280, 800));
            } else {
                scene.setRoot(root);
            }
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load " + fxmlResource + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
