package view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import shared.dto.LobbyDto;

/**
 * Shown after the player joined a lobby that is not yet full.
 * Leave returns to the lobby list; once the server fires {@code onGameStarting()}
 * the router replaces this scene with the board.
 */
public class WaitingViewController implements SceneController {

    @FXML private StackPane rootPane;
    @FXML private Label lobbyNameLabel;
    @FXML private Label countLabel;

    private SceneRouter router;
    private LobbyDto lobby;

    public void bind(SceneRouter router) {
        this.router = router;
    }

    public void setLobby(LobbyDto lobby) {
        showLobbyState(lobby);
    }

    public StackPane root() { return rootPane; }

    public void showLobbyState(LobbyDto lobby) {
        this.lobby = lobby;
        lobbyNameLabel.setText(lobby.name());
        countLabel.setText(lobby.currentPlayers() + " / " + lobby.maxPlayers());
    }

    @FXML
    private void onLeave() {
        if (router.getVirtualServer() != null) router.getVirtualServer().sendLeaveCommand();
        router.toLobby();
    }
}
