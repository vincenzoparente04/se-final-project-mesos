package view;

import network.client.core.ClientMain;
import database.ScoreRecord;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import network.client.core.ClientSession;
import network.client.core.ClientStateListener;
import network.client.core.LocalGameState;
import network.client.core.VirtualServer;
import shared.dto.LobbyDto;
import shared.dto.PlayerDto;
import shared.dto.event.EndGameScoringDto;

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

    private NetworkSetupViewController networkSetupViewController;
    private NickViewController nickViewController;
    private SceneController currentViewController;

    /**
     * Set when setupSession() cannot determine reconnect status synchronously (RMI case).
     * The first onGameStateUpdated() will clear it and navigate to the correct screen.
     */
    private boolean pendingGameReconnect = false;

    // Leaderboard data may arrive before or after the winner screen is shown
    private List<ScoreRecord> pendingLeaderboard;
    private int pendingLeaderboardRank;
    private int pendingLeaderboardPoints;

    public SceneRouter(Stage stage, LocalGameState localState, ClientMain main) {
        this.clientMain = main;
        this.stage = stage;
        this.localState = localState;
    }

    // Bindings set as the user progresses through screens ---------------------------------------------------------------

    //TODO: leave only useful ones
    public void setListener(ClientStateListener l) { this.listener = l; }
    public ClientStateListener listener() { return listener; }
    public VirtualServer getVirtualServer() { return session != null ? session.virtualServer() : null; }
    public String playerName() { return session != null ? session.playerName() : null; }
    public LocalGameState localState() { return localState; }
    public SceneController currentController() { return currentViewController; }
    public StackPane currentRoot() { return currentRoot; }
    public Stage stage() { return stage; }

    // Navigation---------------------------------------------------------------

    public void toSplash() {
        pendingGameReconnect = false;
        load("/org/example/mesos/splash-view.fxml");
    }

    public void toNetworkSetup() {
        pendingGameReconnect = false;
        load("/org/example/mesos/network-setup-view.fxml");
    }

    public void toNick() {
        pendingGameReconnect = false;
        load("/org/example/mesos/nick-view.fxml");
    }

    public void toLobby() {
        load("/org/example/mesos/lobby-view.fxml");
        if (getVirtualServer() != null) getVirtualServer().sendListLobbies();
    }

    public void toWaiting(LobbyDto lobby) {
        pendingGameReconnect = false;
        load("/org/example/mesos/waiting-view.fxml");
        currentViewController.setLobby(lobby);
    }

    public void toTotemPick() {
        pendingGameReconnect = false;
        load("/org/example/mesos/totem-pick-view.fxml");
    }

    public void toBoard() {
        pendingGameReconnect = false;
        load("/org/example/mesos/board/board-view.fxml");
    }

    public void toWinner(List<PlayerDto> players, List<String> winners) {
        load("/org/example/mesos/winner-view.fxml");
        currentViewController.showWinners(players, winners);
        applyPendingLeaderboardIfWinner();
    }

    public void toWinner(List<PlayerDto> players, List<String> winners, EndGameScoringDto scoring) {
        load("/org/example/mesos/winner-view.fxml");
        currentViewController.showWinners(players, winners, scoring);
        applyPendingLeaderboardIfWinner();
    }

    /** Called by the network listener when DB leaderboard data arrives (async). */
    public void offerLeaderboard(List<ScoreRecord> lb, int rank, int pts) {
        pendingLeaderboard = lb;
        pendingLeaderboardRank = rank;
        pendingLeaderboardPoints = pts;
        applyPendingLeaderboardIfWinner();
    }

    private void applyPendingLeaderboardIfWinner() {
        if (pendingLeaderboard == null) return;
        if (currentViewController instanceof WinnerViewController w) {
            w.applyLeaderboard(pendingLeaderboard, pendingLeaderboardRank, pendingLeaderboardPoints);
            pendingLeaderboard = null;
        }
    }

    // FXML loading ---------------------------------------------------------------

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

    // Connection Handling ---------------------------------------------------------------

    public void connect(String transport, String host, int port, NetworkSetupViewController controller) {
        this.networkSetupViewController = controller;
        clientMain.connect(transport, host, port);
    }

    public void connectionEstablished() {
        Platform.runLater(this::toNick);
    }

    public void setName(String name, NickViewController controller) {
        this.nickViewController = controller;
        clientMain.setName(name);
    }

    public void setupSession(String name, VirtualServer vs) {
        this.session = new ClientSession(name, vs);
        Platform.runLater(() -> {
            if (localState.snapshot() != null) {
                // Socket reconnect: state was populated synchronously during handshake.
                navigateByPhase();
            } else {
                // Fresh connection or RMI reconnect: go to lobby for now.
                // If a game state arrives immediately after (RMI reconnect), the
                // pendingGameReconnect flag will redirect us to the correct screen.
                toLobby();
                pendingGameReconnect = true;
            }
        });
    }

    /**
     * Called by onGameStateUpdated() to handle the deferred RMI reconnect routing.
     * Returns true if it navigated (caller should skip the normal update() call and
     * instead update the new controller).
     */
    public boolean applyPendingReconnectIfNeeded() {
        if (!pendingGameReconnect) return false;
        pendingGameReconnect = false;
        navigateByPhase();
        return true;
    }

    private void navigateByPhase() {
        String phase = localState.getPhase();
        if (phase != null && phase.contains("COLOR_CHOOSING_PHASE")) {
            toTotemPick();
        } else {
            toBoard();
        }
    }

    public void connectionErrorHandling(String message) {
            networkSetupViewController.onConnectionError(message);
    }

    public void nickRejected() {
            nickViewController.onNickRejected();
    }
}
