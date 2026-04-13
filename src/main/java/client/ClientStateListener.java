package client;

/**
 * Callback interface implemented by the View (or a presenter) to react
 * to messages coming from the server.
 * All methods are invoked on the JavaFX Application Thread via
 * {@code Platform.runLater} inside {@link ClientThread}.
 */
public interface ClientStateListener {
    void onGameStateUpdated(LocalGameState state);
    void onWaiting(String rawWaitingMessage);
    void onError(String message);
}
