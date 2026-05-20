package network.server.rmi;

import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import network.client.rmi.ClientCallbackRemote;
import network.server.core.LobbyManager;
import network.server.core.VirtualView;

public class RmiVirtualView implements VirtualView {

    private final String playerName;
    private final ClientCallbackRemote callback;
    private final LobbyManager lobbyManager;
    private final ExecutorService senderExecutor;
    private volatile boolean closed = false;

    public RmiVirtualView(String playerName, ClientCallbackRemote callback, LobbyManager lobbyManager) {
        this.playerName = playerName;
        this.callback = callback;
        this.lobbyManager = lobbyManager;
        this.senderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rmi-sender-" + playerName);
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void sendState(GameStateDto dto) {
        if (closed) return;
        senderExecutor.submit(() -> {
            try {
                callback.onState(dto);
                // Game-over is no longer auto-emitted from state: the phase
                // (or the suspension-timeout handler) calls sendGameOver(...)
                // explicitly so the scoring breakdown ships with the winners.
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public void sendEventResolved(EventResolutionDto resolution) {
        if (closed) return;
        senderExecutor.submit(() -> {
            try {
                callback.onEventResolved(resolution);
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public void sendGameOver(List<String> winners, EndGameScoringDto scoring) {
        if (closed) return;
        senderExecutor.submit(() -> {
            try {
                callback.onGameOver(winners, scoring);
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public void sendError(String message) {
        if (closed) return;
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
        if (closed) return;
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
        if (closed) return;
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
        if (closed) return;
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
    public synchronized void close() {
        if(closed) return;
        closed = true;

        senderExecutor.shutdownNow();
    }

    private synchronized void handleDisconnect() {
        if (closed) return;
        closed = true;
        senderExecutor.shutdownNow();
        lobbyManager.onDisconnect(playerName);
    }
}
