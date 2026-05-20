package network.client.rmi;

import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClientCallbackRemote extends Remote {

    void onState(GameStateDto dto) throws RemoteException;

    void onError(String message) throws RemoteException;

    void onLobbyList(List<LobbyDto> lobbies) throws RemoteException;

    void onLobbyState(LobbyDto lobby) throws RemoteException;

    void onGameStarting() throws RemoteException;

    /**
     * Game-over notification. {@code scoring} is {@code null} when the
     * end-game scoring breakdown is not applicable (e.g. suspension-timeout
     * forfeit).
     */
    void onGameOver(List<String> winners, EndGameScoringDto scoring) throws RemoteException;

    /** One-shot notification of a resolved event card (end-of-round / end-of-game). */
    void onEventResolved(EventResolutionDto resolution) throws RemoteException;
}
