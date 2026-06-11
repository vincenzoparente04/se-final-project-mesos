package network.server.core;

import database.ScoreRecord;
import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.util.List;

/**
 * Transport-agnostic server-side projection of a connected client.
 * Each {@code sendXxx} method pushes one message to the client;
 * implementations are responsible for thread safety and delivery ordering.
 * {@link #activateLiveness()} starts the bidirectional heartbeat mechanism
 * once the connection is fully established.
 */
public interface VirtualView {

    /**
     * Pushes the current game state snapshot to the client.
     *
     * @param dto the full game state to deliver
     */
    void sendState(GameStateDto dto);

    /**
     * Delivers an error or notification string to the client.
     *
     * @param message the error string
     */
    void sendError(String message);

    /**
     * Sends the list of open lobbies to the client.
     *
     * @param lobbies the current list of lobby descriptors
     */
    void sendLobbyList(List<LobbyDto> lobbies);

    /**
     * Sends the current state of the lobby the client is in.
     *
     * @param lobby the lobby descriptor to deliver
     */
    void sendLobbyState(LobbyDto lobby);

    /** Notifies the client that the game is about to start. */
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
     * Delivers the end-game leaderboard to the client.
     *
     * @param leaderboard  the top-N score records
     * @param rankPosition this player's global rank
     * @param points       this player's final score
     */
    void sendLeaderboard(List<ScoreRecord> leaderboard, int rankPosition, int points);

    /** Sends a heartbeat ping to the client to keep the connection alive. */
    void sendHeartbeat();

    String getPlayerName();

    /**
     * Starts the bidirectional liveness mechanism (heartbeat sender and inbound
     * watchdog). Must be called after the player has been registered in the
     * connected-player map, so that a timeout fires into an already-ready
     * disconnect pipeline.
     */
    void activateLiveness();

    /** Closes the underlying transport connection and releases all resources. */
    void close();

    /**
     * Updates the liveness timestamp. Called by the transport layer whenever
     * an inbound heartbeat command is received from the client.
     */
    void notifyInbound();
}
