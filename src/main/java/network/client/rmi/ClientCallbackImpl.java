package network.client.rmi;

import shared.dto.GameStateDto;
import shared.dto.LobbyDto;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.List;

import network.client.LocalGameState;
import network.client.ClientStateListener;

/**
 * RMI implementation of the ClientCallbackRemote interface.
 * Client side receiver of server callbacks in the RMI architecture.
 *
 * <p>Design "canale di liveness isolato": solo {@link #onHeartbeat()} aggiorna il sentinel
 * di liveness del {@link RmiVirtualServer}. Gli altri callback applicativi non toccano la
 * liveness, così un eventuale stop del traffico applicativo (ma non degli heartbeat) non
 * produce un falso positivo di disconnessione.
 *
 * <p>L'{@code inboundNotifier} è iniettato al costruttore come {@link Runnable}. Prima che
 * {@link RmiVirtualServer#start()} completi, il notifier è un no-op: in questo modo le rare
 * callback che arrivano tra {@code tryRegisterName} e {@code start} non causano NPE.
 */
public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallbackRemote {

    private final LocalGameState localState;
    private final ClientStateListener listener;
    /** Notifier di liveness: delegato a {@code RmiVirtualServer.notifyInbound()} dopo {@code start()}. */
    private final Runnable inboundNotifier;

    public ClientCallbackImpl(LocalGameState localState, ClientStateListener listener,
                              Runnable inboundNotifier) throws RemoteException {
        super();
        this.localState = localState;
        this.listener = listener;
        this.inboundNotifier = inboundNotifier;
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

    @Override
    public void onHeartbeat() throws RemoteException {
        // Canale di liveness isolato: solo HeartbeatMessage aggiorna il watchdog client-side.
        inboundNotifier.run();
    }
}
