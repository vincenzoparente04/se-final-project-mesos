package network.client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import shared.dto.LobbyDto;
import view.GameViewController;
import view.LobbyViewController;

import java.io.IOException;
import java.util.List;

/**
 * Connects network callbacks to JavaFX UI controllers.
 *
 * Every method is invoked from a background thread (socket reader or RMI thread), so all UI mutations are wrapped in Platform.runLater().
 *
 * Lifecycle:
 *   1. Created in ClientMain.start() together with the lobby scene.
 *   2. setClientController() is called once the network connection is up.
 *   3. onGameStarting() switches the scene and creates the GameViewController.
 */
public class ClientStateListenerGui implements ClientStateListener {

    private final Stage stage;
    private final String playerName;
    private final LobbyViewController lobbyCtrl;

    private VirtualServer virtualServer;
    private GameViewController gameCtrl;

    public ClientStateListenerGui(Stage stage, String playerName, LobbyViewController lobbyCtrl) {
        this.stage = stage;
        this.playerName = playerName;
        this.lobbyCtrl = lobbyCtrl;
    }

    /**
     * Called from the connect-thread once the VirtualServer is ready.
     * Propagates the VirtualServer to the lobby screen so buttons work.
     */
    public void setVirtualServer(VirtualServer vs) {
        this.virtualServer = vs;
        Platform.runLater(() -> lobbyCtrl.setVirtualServer(vs));
    }

    // ─── ClientStateListener callbacks ───────────────────────────────────────

    @Override
    public void onGameStateUpdated(LocalGameState state) {
        Platform.runLater(() -> {
            if (gameCtrl != null) {
                gameCtrl.update(state);
            }else{
                switchToGameScene();
                gameCtrl.update(state);
            }
        });
    }

    @Override
    public void onWaiting(String rawWaitingMessage) {
        Platform.runLater(() ->
                stage.setTitle("Mesos — " + playerName + "  [attesa: " + rawWaitingMessage + "]"));
    }

    @Override
    public void onError(String message) {
        Platform.runLater(() -> {
            if (gameCtrl == null) {
                lobbyCtrl.showError(message);
            } else {
                // In-game errors: show in the title bar for now
                //TODO: maybe add an error label in the game scene?
                stage.setTitle("Mesos — errore: " + message);
            }
        });
    }

    @Override
    public void onLobbyList(List<LobbyDto> lobbies) {
        Platform.runLater(() -> lobbyCtrl.showLobbies(lobbies));
    }

    @Override
    public void onLobbyState(LobbyDto lobby) {
        Platform.runLater(() -> lobbyCtrl.showLobbyState(lobby));
    }

    @Override
    public void onGameStarting() {
        Platform.runLater(this::switchToGameScene);
    }

    @Override
    public void onGameOver(List<String> winners) {
        Platform.runLater(() -> {
            if (gameCtrl != null) gameCtrl.showGameOver(winners);
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> stage.setTitle("Mesos — disconnected"));
    }

    // ─── Scene switch ─────────────────────────────────────────────────────────

    private void switchToGameScene() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/mesos/game-view.fxml"));
            Parent root = loader.load();

            gameCtrl = loader.getController();
            gameCtrl.setVirtualServer(virtualServer);
            gameCtrl.setMyPlayerName(playerName);

            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("Mesos — " + playerName);
        } catch (IOException e) {
            System.err.println("Could not load game-view.fxml: " + e.getMessage());
        }
    }
}
