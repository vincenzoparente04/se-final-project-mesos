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

    /**
     * Avvia il meccanismo di liveness bidirezionale (sender + watchdog).
     * Va chiamato dopo che il player è stato registrato in {@code connectedPlayers},
     * in modo che un eventuale timeout scatti sulla pipeline di disconnect già pronta.
     */
    void activateLiveness();

    void close();
}
