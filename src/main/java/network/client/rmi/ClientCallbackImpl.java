package network.client.rmi;

import shared.dto.GameStateDto;
import shared.dto.LobbyDto;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.List;

import network.client.LocalGameState;
import network.client.clientStateListener.ClientStateListener;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallbackRemote {

    private final LocalGameState localState;
    private final ClientStateListener listener;

    public ClientCallbackImpl(LocalGameState localState, ClientStateListener listener)
            throws RemoteException {
        super();
        this.localState = localState;
        this.listener = listener;
    }

    @Override
    public void onState(GameStateDto dto) throws RemoteException {
        localState.update(dto);
        listener.onGameStateUpdated(localState);
    }

    @Override
    public void onError(String message) throws RemoteException {
        listener.onError(message);
    }

    @Override
    public void onLobbyList(List<LobbyDto> lobbies) throws RemoteException {
        listener.onLobbyList(lobbies);
    }

    @Override
    public void onLobbyState(LobbyDto lobby) throws RemoteException {
        listener.onLobbyState(lobby);
    }

    @Override
    public void onGameStarting() throws RemoteException {
        listener.onGameStarting();
    }

    @Override
    public void onGameOver(String raw) throws RemoteException {
        List<String> winners = raw == null || raw.isBlank()
                ? Collections.emptyList()
                : List.of(raw.split(","));
        listener.onGameOver(winners);
    }
}
