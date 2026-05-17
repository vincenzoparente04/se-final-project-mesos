package network.client.core;

import shared.dto.LobbyDto;

import java.util.List;

/**
 * Callback interface implemented by the View (or a presenter) to react
 * to messages coming from the server.
 * <p>
 * All methods are invoked on the SocketClientThread (network thread).
 * If a JavaFX UI needs to update, implementations should wrap their body
 * in {@code Platform.runLater()} — not this interface.
 */
public interface ClientStateListener {
    void onGameStateUpdated(LocalGameState state);
    void onWaiting(String rawWaitingMessage);
    void onLobbyList(List<LobbyDto> lobbies);
    void onLobbyState(LobbyDto lobby);
    void onGameStarting();
    void onError(String message);
    void onGameOver(List<String> winners);
    void onDisconnected();
}
