package view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import network.client.LocalGameState;
import shared.dto.LobbyDto;
import view.widgets.CreateLobbyDialog;
import view.widgets.ErrorToast;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lobby browser: refresh, create, join.
 * Selecting a lobby and pressing Enter (or double-clicking) joins it;
 * pushing a create-lobby command opens a small dialog for the player count.
 */
public class LobbyViewController implements SceneController {

    @FXML private StackPane rootPane;
    @FXML private Label playerNameLabel;
    @FXML private ListView<LobbyDto> lobbyListView;
    @FXML private Label statusLabel;

    private SceneRouter router;
    private final ObservableList<LobbyDto> items = FXCollections.observableArrayList();

    public void bind(SceneRouter router) {
        this.router = router;
        playerNameLabel.setText("Connected as " + router.playerName());
        lobbyListView.setItems(items);
        lobbyListView.setCellFactory(lv -> new LobbyCell());

        lobbyListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) joinSelected();
        });
        lobbyListView.setOnKeyPressed(e -> {
            if (e.getCode().getName().equalsIgnoreCase("Enter")) joinSelected();
        });

        statusLabel.setText("Loading lobbies…");
    }

    public StackPane root() { return rootPane; }

    // ── Called by the listener ────────────────────────────────────────────

    public void update(LocalGameState state) {
        // Race: a state arrived before onGameStarting routed us.
        if(state.getPhase().contains("COLOR_CHOOSING_PHASE")) {
            router.toTotemPick();
        } else{
            router.toBoard();
        }
    }

    public void showLobbies(List<LobbyDto> lobbies) {
        items.setAll(new ArrayList<>(lobbies));
        statusLabel.setText(lobbies.isEmpty()
                ? "No open lobbies. Create one!"
                : lobbies.size() + " lobbies open. Double-click to join.");
    }

    public void showLobbyState(LobbyDto lobby) {
        router.toWaiting(lobby);
    }

    // ── FXML handlers ─────────────────────────────────────────────────────

    @FXML
    private void onRefresh() {
        router.getVirtualServer().sendListLobbies();
    }

    @FXML
    private void onCreate() {
        Optional<Integer> chosen = new CreateLobbyDialog().showAndWait(rootPane.getScene().getWindow());
        if (chosen.isEmpty()) return;
        router.getVirtualServer().sendCreateLobby(chosen.get());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void joinSelected() {
        LobbyDto selected = lobbyListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ErrorToast.show(rootPane, "Select a lobby first");
            return;
        }
        router.getVirtualServer().sendJoinLobby(selected.id());
    }

    private static final class LobbyCell extends ListCell<LobbyDto> {
        @Override
        protected void updateItem(LobbyDto lobby, boolean empty) {
            super.updateItem(lobby, empty);
            if (empty || lobby == null) {
                setText(null);
                return;
            }
            setText(lobby.name() + "    •    " + lobby.currentPlayers() + " / " + lobby.maxPlayers() + " players");
        }
    }
}
