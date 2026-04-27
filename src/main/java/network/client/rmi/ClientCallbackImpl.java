package client.rmi;

import client.ClientStateListener;
import client.LocalGameState;
import shared.dto.GameStateDto;
import shared.rmi.ClientCallbackRemote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.List;

/**
 * RMI implementation of {@link ClientCallbackRemote}.
 * <p>
 * Exported as a remote object so the server can call back into this JVM.
 * Each method updates the {@link LocalGameState} snapshot and notifies the
 * {@link ClientStateListener} (typically the View).
 * <p>
 * All callback methods are invoked on the RMI dispatch thread. If a JavaFX
 * UI needs to update after the callback, the {@link ClientStateListener}
 * implementation should wrap its body in {@code Platform.runLater()}.
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
    public void onWaiting(String raw) throws RemoteException {
        listener.onWaiting(raw);
    }

    @Override
    public void onGameOver(String raw) throws RemoteException {
        List<String> winners = raw == null || raw.isBlank()
                ? Collections.emptyList()
                : List.of(raw.split(","));
        listener.onGameOver(winners);
    }
}
