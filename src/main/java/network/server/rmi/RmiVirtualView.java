package network.server.rmi;

import shared.dto.GameStateDto;
import shared.dto.LobbyDto;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import network.client.rmi.ClientCallbackRemote;
import network.server.core.DisconnectListener;
import network.server.core.VirtualView;

public class RmiVirtualView implements VirtualView {

    private final String playerName;
    private final ClientCallbackRemote callback;
    private final ExecutorService senderExecutor;
    private final AtomicReference<DisconnectListener> onDisconnect =
            new AtomicReference<>(ignored -> {});
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public RmiVirtualView(String playerName,
                          ClientCallbackRemote callback) {
        this.playerName = playerName;
        this.callback = callback;
        this.senderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rmi-sender-" + playerName);
            t.setDaemon(true);
            return t;
        });    }

    public void setOnDisconnect(DisconnectListener handler) {
        onDisconnect.set(handler);
    }

    @Override
    public void sendState(GameStateDto dto) {
        if (closed.get()) return;

        senderExecutor.submit(() -> {
            try {
                callback.onState(dto);
                if (dto.winners != null && !dto.winners.isEmpty()) {
                    callback.onGameOver(String.join(",", dto.winners));
                }
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public void sendError(String message) {
        if (closed.get()) return;

        senderExecutor.submit(() -> {
            try {
                callback.onError(message);
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public void sendLobbyList(List<LobbyDto> lobbies) {
        if (closed.get()) return;

        senderExecutor.submit(() -> {
            try {
                callback.onLobbyList(lobbies);
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public void sendLobbyState(LobbyDto lobby) {
        if (closed.get()) return;

        senderExecutor.submit(() -> {
            try {
                callback.onLobbyState(lobby);
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public void sendGameStarting() {
        if (closed.get()) return;

        senderExecutor.submit(() -> {
            try {
                callback.onGameStarting();
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void close() {
        closed.set(true);
    }

    private void handleDisconnect() {
        if (closed.compareAndSet(false, true)) {
            onDisconnect.get().onDisconnected(playerName);
        }
    }
}
