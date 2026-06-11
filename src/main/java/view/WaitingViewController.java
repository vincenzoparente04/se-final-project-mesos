package view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import shared.dto.LobbyDto;

/**
 * Shown after the player joined a lobby that is not yet full.
 * Leave returns to the lobby list; once the server sends a game state with the COLOR_CHOOSING_PHASE,
 * the router replaces this scene with the board.
 */
public class WaitingViewController implements SceneController {

    @FXML private StackPane rootPane;
    @FXML private Label lobbyNameLabel;
    @FXML private Label countLabel;

    private SceneRouter router;
    private LobbyDto lobby;

    /**
     * Binds the router to this controller when this controller is created to make it possible to reference it.
     * @param router the SceneRouter
     */
    public void bind(SceneRouter router) {
        this.router = router;
    }

    /**
     * Called by the {@link SceneRouter} to display the current lobby state in the view.
     * @param lobby
     */
    public void setLobby(LobbyDto lobby) {
        showLobbyState(lobby);
    }

    /**
     * called by the {@link SceneRouter} in the loadFXML method.
     * @return the root StackPane.
     */
    public StackPane root() { return rootPane; }

    /**
     * Modifies the values in the {@link resources.org.examples.mesos#waiting-view waiting_view} to reflect the current lobby state.
     * @param lobby current lobby DTO
     */
    public void showLobbyState(LobbyDto lobby) {
        this.lobby = lobby;
        lobbyNameLabel.setText(lobby.name());
        countLabel.setText(lobby.currentPlayers() + " / " + lobby.maxPlayers());
    }

    /**
     * logic that happens after the button leave lobby is clicked. Send the leave command to the server and
     * returns to lobby view again
     */
    @FXML
    private void onLeave() {
        router.getVirtualServer().sendLeaveCommand();
        router.toLobby();
    }
}
