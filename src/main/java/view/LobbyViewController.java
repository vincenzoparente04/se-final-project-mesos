package view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import network.client.core.VirtualServer;
import javafx.scene.layout.StackPane;
import network.client.core.LocalGameState;
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

    /** The root pane of the lobby view screen layout. */
    @FXML private StackPane rootPane;
    /** Label to show the name of the player*/
    @FXML private Label playerNameLabel;
    /** List of the lobbies opened on the server*/
    @FXML private ListView<LobbyDto> lobbyListView;
    /** Label to report change in status and errors */
    @FXML private Label statusLabel;

    /** The router used to navigate between scenes. */
    private SceneRouter router;
    
    /** The observable list backing the lobby ListView, containing the currently available lobbies. */
    private final ObservableList<LobbyDto> items = FXCollections.observableArrayList();

    /**
     * Binds the {@link SceneRouter} to this controller and initializes the label, the list view and listeners.
     * Starts music.
     * @param router actual {@link SceneRouter}
     */
    public void bind(SceneRouter router) {
        this.router = router;
        playerNameLabel.setText("Connected as " + router.playerName());

        //binds lobbyListView and items
        lobbyListView.setItems(items);
        //'teaches' the listView how to 'draw' the cell with the chosen style
        lobbyListView.setCellFactory(lv -> new LobbyCell());

        lobbyListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) joinSelected();
        });
        lobbyListView.setOnKeyPressed(e -> {
            if (e.getCode().getName().equalsIgnoreCase("Enter")) joinSelected();
        });

        statusLabel.setText("Loading…");

        MusicManager.getInstance().playRandom("music/scaricamusicayoutube");
    }

    /**
     * Returns the root node of this controller's view.
     */
    public StackPane root() { return rootPane; }

    // Called by the listener ---------------------------------

    /**
     * Called by the {@link network.client.core.ClientStateListenerGui} when the server sends a new game state.
     * Asks the {@link SceneRouter} to change the view accordingly to the game state.
     * @param state the game state given by the server
     */
    public void update(LocalGameState state) {
        // Race: a state arrived before onGameStarting routed us.
        if(state.getPhase().contains("COLOR_CHOOSING_PHASE")) {
            router.toTotemPick();
        } else{
            if (state.isGameOver()) {
                router.toWinner(state.getPlayers(), state.getWinners());
                return;
            }
            router.toBoard();
        }
    }


    /**
     * Called by the {@link network.client.core.ClientStateListenerGui} when the server sends a new lobby list.
     * Updates the lobby ListView to show the current open lobbies.
     * @param lobbies list of lobbies DTO
     */
    public void showLobbies(List<LobbyDto> lobbies) {
        items.setAll(new ArrayList<>(lobbies));
        statusLabel.setText(lobbies.isEmpty() ? "No open lobbies.\nCreate one!" : "");
    }

    /**
     * When the player is waiting in a lobby for a game to start, this methodo refresh the GUI.
     * @param lobby the updated state of the lobby
     */
    public void showLobbyState(LobbyDto lobby) {
        router.toWaiting(lobby);
    }

    // FXML handlers --------------------------------------------

    /**
     * Called when the user clicks on the "Create New Lobby" button.
     * Show the {@link CreateLobbyDialog} and, after the user choose the number of player of the new game,
     * sends a create-lobby command to the server.
     */
    @FXML
    private void onCreate() {
        Optional<Integer> chosen = new CreateLobbyDialog().showAndWait(rootPane.getScene().getWindow());
        if (chosen.isEmpty()) return;
        router.getVirtualServer().sendCreateLobby(chosen.get());
    }

    // Helpers --------------------------------------------------------

    /**
     * Called after the double click on the chosen lobby or after the selection and the press of the Enter key.
     * If the user has selected a lobby, a join-lobby command is sent to the server via {@link SceneRouter} with the id of the selected lobby.
     * If not an error toast is shown.
     */
    private void joinSelected() {
        LobbyDto selected = lobbyListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            ErrorToast.show(rootPane, "Select a lobby first");
            return;
        }
        router.getVirtualServer().sendJoinLobby(selected.id());

    }

    /**
     * Custom {@link ListCell} implementation to restyle the display of each lobby in the ListView.
     * It shows the lobby name and the current/maximum player count in a horizontal layout.
     */
    private static final class LobbyCell extends ListCell<LobbyDto> {
        
        /** The label displaying the name of the lobby. */
        private final Label nameLabel = new Label();
        
        /** The label displaying the current and maximum number of players in the lobby. */
        private final Label playersLabel = new Label();
        
        /** The horizontal box layout containing the name and players labels. */
        private final javafx.scene.layout.HBox hbox;

        /**
         * Constructs a new {@code LobbyCell} with initialized labels and layout.
         * Sets up the CSS classes and alignment for the cell components.
         */
        LobbyCell() {
            nameLabel.getStyleClass().add("mesos-label-bold");
            nameLabel.setStyle("-fx-font-size: 32;");
            playersLabel.getStyleClass().add("mesos-label");
            hbox = new javafx.scene.layout.HBox(50, nameLabel, playersLabel);
            hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        /**
         * Updates the visual representation of the cell based on the given {@link LobbyDto}.
         *
         * @param lobby the {@link LobbyDto} item associated with this cell
         * @param empty {@code true} if this cell represents an empty space in the list, {@code false} otherwise
         */
        @Override
        protected void updateItem(LobbyDto lobby, boolean empty) {
            super.updateItem(lobby, empty);
            setText(null);
            if (empty || lobby == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(lobby.name());
                playersLabel.setText(lobby.currentPlayers() + " / " + lobby.maxPlayers() + " players");
                setGraphic(hbox);
            }
        }
    }
}
