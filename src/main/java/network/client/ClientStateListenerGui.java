package client;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.List;

public class ClientStateListenerGui implements ClientStateListener{
    private Stage primaryStage;
    private String playerName;

    public ClientStateListenerGui(Stage primaryStage,  String playerName) {
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
    public void onWaiting(String rawWaitingMessage) {
        Platform.runLater(() -> {
            primaryStage.setTitle("Mesos — " + playerName + " [waiting " + rawWaitingMessage + "]");
            System.out.println("[WAITING] " + rawWaitingMessage);
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
