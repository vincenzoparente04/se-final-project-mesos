package integration;

import database.ScoreRecord;
import network.server.core.VirtualView;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.util.List;

public class FakeVirtualView implements VirtualView {
    @Override
    public void sendState(shared.dto.GameStateDto dto) {
    }

    @Override
    public void sendError(String message) {
    }

    @Override
    public void sendLobbyList(List<LobbyDto> lobbies) {
    }

    @Override
    public void sendLobbyState(LobbyDto lobby) {
    }

    @Override
    public void sendGameStarting() {
    }

    @Override
    public void sendEventResolved(EventResolutionDto resolution) {
    }

    @Override
    public void sendGameOver(List<String> winners, EndGameScoringDto scoring) {
    }

    @Override
    public void sendLeaderboard(List<ScoreRecord> leaderboard, int rankPosition, int points) {
    }

    @Override
    public void sendHeartbeat() {
    }

    @Override
    public String getPlayerName() {
        return null;
    }

    @Override
    public void activateLiveness() {

    }

    @Override
    public void notifyInbound() {
    }

    @Override
    public void close() {
    }
}
