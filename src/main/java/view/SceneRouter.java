package view;

import network.client.core.ClientMain;
import database.ScoreRecord;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import network.client.core.ClientSession;
import network.client.core.LocalGameState;
import network.client.core.VirtualServer;
import shared.dto.LobbyDto;
import shared.dto.PlayerDto;
import shared.dto.event.EndGameScoringDto;
import view.widgets.ErrorToast;
import view.widgets.EventResolutionOverlay;

import java.util.List;

/**
 * Single navigation hub for the whole GUI.
 * Each screen calls back into the router to move forward (or backward),
 * the {@link network.client.core.ClientStateListenerGui} uses it to address the {@link ViewController} that is on screen
 * and to overlay toasts on top of the current root.
 * It's created by the {@link ClientMain} and passed to each controller.
 * Handles the followings:
 * <ul>
 *     <li>Move from one ViewController to another and loads its FXML</li>
 *     <li>Handles commands and errors for joining the server between the {@link NickViewController} and the
 *     {@link ClientMain}</li>
 * </ul>
 */
public class SceneRouter {

    private final ClientMain clientMain;
    private final Stage stage;
    private final LocalGameState localState;

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

    /**
     * constructs the router
     */
    public SceneRouter(Stage stage, LocalGameState localState, ClientMain main) {
        this.clientMain = main;
        this.stage = stage;
        this.localState = localState;
    }

    // Bindings set as the user progresses through screens ---------------------------------------------------------------

    public VirtualServer getVirtualServer() { return session != null ? session.virtualServer() : null; }
    public String playerName() { return session != null ? session.playerName() : null; }
    public LocalGameState localState() { return localState; }
    public SceneController currentController() { return currentViewController; }
    public StackPane currentRoot() { return currentRoot; }
    public Stage stage() { return stage; }

    // Navigation---------------------------------------------------------------

    /**
     * Navigates to the splash screen. This is the first screen shown when the application starts.
     */
    public void toSplash() {
        pendingGameReconnect = false;
        load("/org/example/mesos/splash-view.fxml");
    }

    /**
     * Navigates to the network screen where the user can choose connection settings and connect to the server.
     */
    public void toNetworkSetup() {
        pendingGameReconnect = false;
        load("/org/example/mesos/network-setup-view.fxml");
    }

    /**
     * Navigates to the nickname choosing screen.
     */
    public void toNick() {
        pendingGameReconnect = false;
        load("/org/example/mesos/nick-view.fxml");
    }

    /**
     * Navigates to the lobby screen, in which user can choose which game to join
     */
    public void toLobby() {
        EventResolutionOverlay.reset();
        load("/org/example/mesos/lobby-view.fxml");
        if (getVirtualServer() != null) getVirtualServer().sendListLobbies();
    }

    /**
     * Navigates to the screen in which the user waits for the game to start after joining a lobby.
     * @param lobby the lobby the user is joining, used to display lobby info while waiting for the game to start
     */
    public void toWaiting(LobbyDto lobby) {
        pendingGameReconnect = false;
        load("/org/example/mesos/waiting-view.fxml");
        currentViewController.setLobby(lobby);
    }

    /**
     * Navigates to the screen in which the user can choose the color of its totem
     */
    public void toTotemPick() {
        pendingGameReconnect = false;
        load("/org/example/mesos/totem-pick-view.fxml");
    }

    /**
     * Navigates to the game board
     */
    public void toBoard() {
        pendingGameReconnect = false;
        load("/org/example/mesos/board/board-view.fxml");
    }

    /**
     * Navigates to the winner view, showing the point info of the game.
     * Called when the game terminates early due to disconnection of player, there are no EndGameScoringDto to show.
     * @param players the list of players DTO in the game
     * @param winners the list of the winners of the games, could be one or more than one if it's a draw
     */
    public void toWinner(List<PlayerDto> players, List<String> winners) {
        load("/org/example/mesos/winner-view.fxml");
        currentViewController.showWinners(players, winners);
        applyPendingLeaderboardIfWinner();
    }

    /**
     * Navigates to the winner view, showing the point info of the game.
     * Called when the game terminates normally, after the 10th round.
     * @param players the list of players DTO in the game
     * @param winners the list of the winners of the games, could be one or more than one if it's a draw
     * @param scoring DTO reporting the scoring of each player at the end of the game, of each category
     */
    public void toWinner(List<PlayerDto> players, List<String> winners, EndGameScoringDto scoring) {
        load("/org/example/mesos/winner-view.fxml");
        currentViewController.showWinners(players, winners, scoring);
        applyPendingLeaderboardIfWinner();
    }

    /** Called by the network listener when DB leaderboard data arrives (async).
     *  If the winner screen is already shown, applies the data to it. Otherwise, stores it in pending variables to be applied when the winner screen is shown.
     * @param lb the list of all players and their points, sorted by points descending
     * @param rank the rank of the current player in the leaderboard
     * @param pts the points of the current player in the leaderboard
     */
    public void offerLeaderboard(List<ScoreRecord> lb, int rank, int pts) {
        pendingLeaderboard = lb;
        pendingLeaderboardRank = rank;
        pendingLeaderboardPoints = pts;
        applyPendingLeaderboardIfWinner();
    }

    /**
     * If the winner screen is already shown and pending leaderboard data is available,
     * applies the data to the current winner screen.
     */
    private void applyPendingLeaderboardIfWinner() {
        if (pendingLeaderboard == null) return;
        if (currentViewController instanceof WinnerViewController w) {
            w.applyLeaderboard(pendingLeaderboard, pendingLeaderboardRank, pendingLeaderboardPoints);
            pendingLeaderboard = null;
        }
    }

    // FXML loading ---------------------------------------------------------------

    /**
     * Loads the FXML file at the given path and sets it as the current scene.
     * @param fxmlResource path to the FXML of each screen
     */
    private void load(String fxmlResource) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlResource));
            loader.load();
            SceneController ctrl = loader.getController();
            StackPane root = ctrl.root();

            ctrl.bind(this);

            this.currentViewController = ctrl;
            this.currentRoot = root;

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
        } catch (Exception e) {
            System.err.println("Failed to load " + fxmlResource + ": " + e.getMessage());
            e.printStackTrace();
            if (currentRoot != null) ErrorToast.show(currentRoot, "Failed to load screen");
        }
    }

    // Connection Handling ---------------------------------------------------------------

    /**
     *Called by the network listener when the connection to the server is lost.
     * Navigates back to the network setup screen and shows a toast with the error message.
     * @param transport type of connection: RMI or SOCKET
     * @param host host ip
     * @param port server port number
     * @param controller NetworkSetupViewController used to bind the sceneRouter with the network setup screen, so that the user can try reconnecting
     */
    public void connect(String transport, String host, int port, NetworkSetupViewController controller) {
        this.networkSetupViewController = controller;
        clientMain.connect(transport, host, port);
    }

    /**
     * Called by the network listener when the connection to the server is successfully established.
     * Asks the sceneRouter to route to the nickname choosing screen.
     */
    public void connectionEstablished() {
        Platform.runLater(this::toNick);
    }

    /**
     * Called by the nickViewController when the player chooses its name. Asks the clientMain to set the name.
     * @param name name chosen by the player
     * @param controller NickViewController used to bind the sceneRouter with the nickname choosing screen, so that the user can try choosing another nickname if the chosen one is already taken
     */
    public void setName(String name, NickViewController controller) {
        this.nickViewController = controller;
        clientMain.setName(name);
    }

    /**
     * Called by ClientMain to set up the virtual server reference in the scene router.
     * Then starts the navigation from the lobby on.
     * @param name the name of the player accepted by the server
     * @param vs the reference to the virtual server
     */
    public void setupSession(String name, VirtualServer vs) {
        this.session = new ClientSession(name, vs);
        Platform.runLater(() -> {
            if (localState.snapshot() != null) {
                navigateByPhase();
            } else {
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

    /**
     * Normal navigation function that check if the started game is still in the color choosing phase, or it's in the
     * game phases.
     */
    private void navigateByPhase() {
        String phase = localState.getPhase();
        if (phase != null && phase.contains("COLOR_CHOOSING_PHASE")) {
            toTotemPick();
        } else {
            toBoard();
        }
    }

    /**
     * called by {@link ClientMain} when the connection to the server is unsuccessful.
     * @param message the server message given to explain the error
     */
    public void connectionErrorHandling(String message) {
            networkSetupViewController.onConnectionError(message);
    }

    /**
     * Called by the network listener when the chosen nickname is already taken by another player.
     * Asks the nickViewController to let the user choose another nickname.
     */
    public void nickRejected() {
            nickViewController.onNickRejected();
    }
}
