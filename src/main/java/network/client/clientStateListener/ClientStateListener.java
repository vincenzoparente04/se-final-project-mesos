package network.client.clientStateListener;

import shared.dto.LobbyDto;

import java.util.List;

import network.client.LocalGameState;

public interface ClientStateListener {
    void onGameStateUpdated(LocalGameState state);
    void onLobbyList(List<LobbyDto> lobbies);
    void onLobbyState(LobbyDto lobby);
    void onGameStarting();
    void onError(String message);
    void onGameOver(List<String> winners);
    void onDisconnected();
}
