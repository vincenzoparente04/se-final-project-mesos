package network.server.core;

import shared.dto.GameStateDto;
import shared.dto.LobbyDto;

import java.util.List;

public interface VirtualView {

    void sendState(GameStateDto dto);

    void sendError(String message);

    void sendLobbyList(List<LobbyDto> lobbies);

    void sendLobbyState(LobbyDto lobby);

    void sendGameStarting();

    String getPlayerName();

    void close();
}
