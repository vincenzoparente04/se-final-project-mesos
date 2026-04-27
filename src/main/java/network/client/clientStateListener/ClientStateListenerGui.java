package network.client.clientStateListener;

import javafx.application.Platform;
import javafx.stage.Stage;
import network.client.LocalGameState;
import shared.dto.LobbyDto;

import java.util.List;

public class ClientStateListenerGui implements ClientStateListener {

    private final Stage primaryStage;
    private final String playerName;

    public ClientStateListenerGui(Stage primaryStage, String playerName) {
        this.primaryStage = primaryStage;
        this.playerName = playerName;
    }

    @Override
    public void onGameStateUpdated(LocalGameState state) {
        Platform.runLater(() ->
                System.out.println("[STATE] phase=" + state.getPhase()
                        + " round=" + state.getCurrentRound()
                        + " currentPlayer=" + state.getCurrentPlayerName()));
    }

    @Override
    public void onLobbyList(List<LobbyDto> lobbies) {
        Platform.runLater(() -> System.out.println("[LOBBIES] " + lobbies.size() + " open"));
    }

    @Override
    public void onLobbyState(LobbyDto lobby) {
        Platform.runLater(() -> {
            primaryStage.setTitle("Mesos — " + playerName
                    + " [lobby: " + lobby.currentPlayers() + "/" + lobby.maxPlayers() + "]");
            System.out.println("[LOBBY] " + lobby.name()
                    + " — " + lobby.currentPlayers() + "/" + lobby.maxPlayers());
        });
    }

    @Override
    public void onGameStarting() {
        Platform.runLater(() -> {
            primaryStage.setTitle("Mesos — " + playerName + " [game starting]");
            System.out.println("[GAME] Starting!");
        });
    }

    @Override
    public void onError(String message) {
        Platform.runLater(() -> System.out.println("[ERROR] " + message));
    }

    @Override
    public void onGameOver(List<String> winners) {
        Platform.runLater(() -> System.out.println("[GAME OVER] Winner(s): " + winners));
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            primaryStage.setTitle("Mesos — disconnected");
            System.out.println("[DISCONNECTED] Connection to server lost.");
        });
    }
}
