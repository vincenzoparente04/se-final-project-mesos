package network.client.core;

import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

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

    /** One-shot notification: an event card has been resolved server-side. */
    void onEventResolved(EventResolutionDto resolution);

    /**
     * Game-over notification. {@code scoring} is {@code null} when the
     * end-game scoring breakdown is not applicable (e.g. suspension-timeout
     * forfeit).
     */
    void onGameOver(List<String> winners, EndGameScoringDto scoring);

    void onDisconnected();
}
