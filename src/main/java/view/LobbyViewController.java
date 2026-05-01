package view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import network.client.ClientController;
import shared.dto.LobbyDto;

import java.util.ArrayList;
import java.util.List;

public class LobbyViewController {

    @FXML private Label playerNameLabel;
    @FXML private ListView<String> lobbyListView;
    @FXML private TextField maxPlayersField;
    @FXML private Label statusLabel;

    private ClientController clientController;
    private final List<LobbyDto> currentLobbies = new ArrayList<>();

    public void init(String playerName) {
        playerNameLabel.setText(playerName);
    }

    public void setClientController(ClientController cc) {
        this.clientController = cc;
        setStatus("Connected. Find a lobby or create one.", false);
    }

    // ─── Called by ClientStateListenerGui (already on FX thread) ───────────

    public void showLobbies(List<LobbyDto> lobbies) {
        currentLobbies.clear();
        currentLobbies.addAll(lobbies);

        lobbyListView.getItems().clear();
        for (LobbyDto lobby : lobbies) {
            lobbyListView.getItems().add(
                    lobby.name() + "  [" + lobby.currentPlayers() + "/" + lobby.maxPlayers() + "]"
                            + "  id:" + lobby.id()); //lobby id should be seen from the player?
        }
        setStatus(lobbies.size() + " available lobby.", false);
    }

    public void showLobbyState(LobbyDto lobby) {
        setStatus("In lobby \"" + lobby.name() + "\"  -  "
                + lobby.currentPlayers() + "/" + lobby.maxPlayers()
                + " players  (waiting...)", false);
    }

    public void showError(String message) {
        setStatus("Error: " + message, true);
    }

    // ─── FXML button handlers ───────────────────────────────────────────────

    @FXML
    private void onListLobbies() {
        if (clientController == null) return;
        clientController.onListLobbies();
    }

    @FXML
    private void onJoinLobby() {
        if (clientController == null) return;
        int idx = lobbyListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= currentLobbies.size()) {
            setStatus("Select a lobby from the list.", true);
            return;
        }
        clientController.onJoinLobby(currentLobbies.get(idx).id());
    }

    @FXML
    private void onCreateLobby() {
        if (clientController == null) return;
        String text = maxPlayersField.getText().trim();
        try {
            int max = Integer.parseInt(text);
            if(max>5||max<2){
                setStatus("Number of players must be between 2 and 5", true);
                return;
            }
            clientController.onCreateLobby(max);
        } catch (NumberFormatException e) {
            setStatus("Insert a valid number of players", true);
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: gray;");
    }
}
