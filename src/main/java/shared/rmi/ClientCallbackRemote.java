package shared.rmi;

import shared.dto.GameStateDto;
import shared.dto.LobbyDto;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClientCallbackRemote extends Remote {

    void onState(GameStateDto dto) throws RemoteException;

    void onError(String message) throws RemoteException;

    void onLobbyList(List<LobbyDto> lobbies) throws RemoteException;

    void onLobbyState(LobbyDto lobby) throws RemoteException;

    void onGameStarting() throws RemoteException;

    void onGameOver(String raw) throws RemoteException;
}
