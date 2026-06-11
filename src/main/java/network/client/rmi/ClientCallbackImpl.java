package network.client.rmi;

import database.ScoreRecord;
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
 * RMI implementation of {@link ClientCallbackRemote}: the client-side receiver of
 * server callbacks in the RMI transport. Each callback updates the local state
 * and/or notifies the {@link ClientStateListener}.
 *
 * <p>"Isolated liveness channel" design: only {@link #onHeartbeat()} refreshes the
 * {@link RmiVirtualServer}'s liveness sentinel. The application callbacks do not
 * touch liveness, so a stop of application traffic (but not of heartbeats) does not
 * produce a false-positive disconnect.
 *
 * <p>The {@code inboundNotifier} is injected into the constructor as a {@link Runnable}.
 * Until {@link RmiVirtualServer#start()} completes it is a no-op, so the rare
 * callbacks arriving between {@code tryRegisterName} and {@code start} cause no NPE.
 */
public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallbackRemote {

    /** Local mirror of the game state, updated on {@link #onState}. */
    private final LocalGameState localState;
    /** Listener notified of inbound server events. */
    private final ClientStateListener listener;
    /** Liveness notifier: delegates to {@code RmiVirtualServer.notifyInbound()} after {@code start()}. */
    private final Runnable inboundNotifier;

    /**
     * @param localState the local state to update from server snapshots
     * @param listener the listener to notify of inbound events
     * @param inboundNotifier the liveness notifier (a no-op until {@code start()})
     * @throws RemoteException if exporting this remote object fails
     */
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
    public void onGameOver(List<String> winners, EndGameScoringDto scoring) throws RemoteException {
        listener.onGameOver(winners != null ? winners : Collections.emptyList(), scoring);
    }

    @Override
    public void onLeaderboardUpdate(List<ScoreRecord> leaderboard, int rankPosition, int points) throws RemoteException{
        listener.onLeaderboardUpdate(leaderboard, rankPosition, points);
    }

    @Override
    public void onEventResolved(EventResolutionDto resolution) throws RemoteException {
        listener.onEventResolved(resolution);
    }

    @Override
    public void onHeartbeat() throws RemoteException {
        // Isolated liveness channel: only the heartbeat refreshes the client-side watchdog.
        inboundNotifier.run();
    }
}
