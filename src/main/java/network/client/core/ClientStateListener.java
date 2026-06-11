package network.client.core;

import database.ScoreRecord;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.rmi.RemoteException;
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

    /**
     * The local game state has been updated from a fresh server snapshot.
     *
     * @param state the updated local game state (read it via {@code snapshot()})
     */
    void onGameStateUpdated(LocalGameState state);

    /**
     * Pre-game waiting update: how many players are in the lobby vs. expected.
     *
     * @param rawWaitingMessage the raw {@code "current:expected"} payload
     */
    void onWaiting(String rawWaitingMessage);

    /**
     * The list of open lobbies has changed (lobby-browser view).
     *
     * @param lobbies the lobbies currently joinable
     */
    void onLobbyList(List<LobbyDto> lobbies);

    /**
     * The state of the player's own lobby has changed.
     *
     * @param lobby the current snapshot of the player's lobby
     */
    void onLobbyState(LobbyDto lobby);

    /** The player's lobby has filled up and the game is starting. */
    void onGameStarting();

    /**
     * The server reported an error or status message.
     *
     * @param message the protocol-defined error/status text
     */
    void onError(String message);

    /**
     * One-shot notification: an event card has been resolved server-side.
     *
     * @param resolution the resolved-event payload to display
     */
    void onEventResolved(EventResolutionDto resolution);

    /**
     * Game-over notification. {@code scoring} is {@code null} when the
     * end-game scoring breakdown is not applicable (e.g. suspension-timeout
     * forfeit).
     *
     * @param winners the winning player names (may be empty on a forfeit)
     * @param scoring the end-game scoring breakdown, or {@code null} if not applicable
     */
    void onGameOver(List<String> winners, EndGameScoringDto scoring);

    /**
     * The post-game leaderboard and this player's placement are available.
     *
     * @param leaderboard the top score records
     * @param rankPosition this player's 1-based position in the ranking
     * @param points this player's final score
     */
    void onLeaderboardUpdate(List<ScoreRecord> leaderboard, int rankPosition, int points);

    /** The connection to the server has been lost or closed. */
    void onDisconnected();
}
