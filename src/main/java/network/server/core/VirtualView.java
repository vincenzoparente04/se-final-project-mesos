package network.server.core;

import database.ScoreRecord;
import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.util.List;

/**
 * Server-side, transport-agnostic outbound channel to a single client.
 * <p>
 * Each connected player owns one {@code VirtualView}; the game- and lobby-threads
 * push messages to the client exclusively through it. Implementations
 * ({@code SocketVirtualView}, {@code RmiVirtualView}) serialise every {@code sendXxx}
 * call onto a dedicated single-thread executor, so callers never block on a slow
 * client and outbound messages to a given client are never interleaved.
 */
public interface VirtualView {

    /**
     * Sends the latest game-state snapshot to the client using a separate thread for both implementations.
     *
     * @param dto the game-state snapshot to transmit
     */
    void sendState(GameStateDto dto);

    void sendError(String message);

    /**
     * Sends the list of open lobbies (lobby-browser view).
     *
     * @param lobbies the lobbies currently joinable
     */
    void sendLobbyList(List<LobbyDto> lobbies);

    /**
     * Sends the current state of the single lobby the player belongs to.
     *
     * @param lobby the snapshot of the player's lobby
     */
    void sendLobbyState(LobbyDto lobby);

    /** Notifies the client that its lobby has filled up and the game is starting. */
    void sendGameStarting();

    /** One-shot notification of a resolved event card (end-of-round / end-of-game). */
    void sendEventResolved(EventResolutionDto resolution);

    /**
     * Explicit game-over notification. Carries the winners and optionally the
     * end-game scoring breakdown ({@code null} for the suspension-timeout
     * forfeit case).
     */
    void sendGameOver(List<String> winners, EndGameScoringDto scoring);

    /**
     * Sends the post-game leaderboard together with this player's placement.
     *
     * @param leaderboard the top score records
     * @param rankPosition this player's 1-based position in the ranking
     * @param points this player's final score
     */
    void sendLeaderboard(List<ScoreRecord> leaderboard, int rankPosition, int points);

    /** Sends a liveness heartbeat; invoked periodically by the liveness sentinel. */
    void sendHeartbeat();

    /** @return the server-wide name of the player this view belongs to */
    String getPlayerName();

    /**
     * Starts the bidirectional liveness mechanism (heartbeat sender + watchdog).
     * Must be called only after the player has been registered in
     * {@code connectedPlayers}, so that a liveness timeout fires onto an
     * already-wired disconnect pipeline.
     */
    void activateLiveness();

    /**
     * Closes the view and releases its resources (sender executor, sentinel and
     * underlying transport). Idempotent.
     */
    void close();

    /**
     * Refreshes the inbound-liveness timestamp. Must be called only on arrival of
     * a heartbeat from the client (isolated liveness channel).
     */
    void notifyInbound();
}
