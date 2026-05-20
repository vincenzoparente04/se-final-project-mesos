package network.client.rmi;

import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.List;

import network.client.core.LocalGameState;
import network.client.core.ClientStateListener;

/**
 * RMI implementation of the ClientCallbackRemote interface.
 * Client side receiver of server callbacks in the RMI architecture.
 */
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
    public void onGameOver(List<String> winners, EndGameScoringDto scoring) throws RemoteException {
        listener.onGameOver(winners != null ? winners : Collections.emptyList(), scoring);
    }

    @Override
    public void onEventResolved(EventResolutionDto resolution) throws RemoteException {
        listener.onEventResolved(resolution);
    }
}
