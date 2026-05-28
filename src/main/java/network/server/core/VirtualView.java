package network.server.core;

import database.ScoreRecord;
import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.util.List;

public interface VirtualView {

    void sendState(GameStateDto dto);

    void sendError(String message);

    void sendLobbyList(List<LobbyDto> lobbies);

    void sendLobbyState(LobbyDto lobby);

    void sendGameStarting();

    /** One-shot notification of a resolved event card (end-of-round / end-of-game). */
    void sendEventResolved(EventResolutionDto resolution);

    /**
     * Explicit game-over notification. Carries the winners and optionally the
     * end-game scoring breakdown ({@code null} for the suspension-timeout
     * forfeit case).
     */
    void sendGameOver(List<String> winners, EndGameScoringDto scoring);

    void sendLeaderboard(List<ScoreRecord> leaderboard, int rankPosition, int points);

    void sendHeartbeat();

    String getPlayerName();

    /**
     * Avvia il meccanismo di liveness bidirezionale (sender + watchdog).
     * Va chiamato dopo che il player è stato registrato in {@code connectedPlayers},
     * in modo che un eventuale timeout scatti sulla pipeline di disconnect già pronta.
     */
    void activateLiveness();

    void close();

    void notifyInbound();
}
