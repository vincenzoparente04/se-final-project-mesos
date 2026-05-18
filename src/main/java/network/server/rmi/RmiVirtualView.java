package network.server.rmi;

import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.liveness.LivenessSentinel;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import network.client.rmi.ClientCallbackRemote;
import network.server.core.LobbyManager;
import network.server.core.VirtualView;

public class RmiVirtualView implements VirtualView {

    /**
     * Periodo di invio heartbeat server→client.
     * Invariante: {@code TIMEOUT_MS > 2 * SEND_INTERVAL_MS} per tollerare il jitter di scheduling.
     */
    /**
     * Intervalli del sentinel server-side RMI.
     * Invariante: {@code TIMEOUT_MS > 2 * SEND_INTERVAL_MS} per tollerare il jitter di scheduling
     * e ritardi temporanei dovuti a messaggi applicativi grandi che impegnano il sender.
     * Il detection time massimo è {@code TIMEOUT_MS + CHECK_INTERVAL_MS = 12s}.
     */
    private static final long SEND_INTERVAL_MS  = 2_000L;
    private static final long CHECK_INTERVAL_MS = 5_000L;
    private static final long TIMEOUT_MS        = 15_000L;

    private final String playerName;
    private final ClientCallbackRemote callback;
    private final LobbyManager lobbyManager;
    private final ExecutorService senderExecutor;
    private final LivenessSentinel sentinel;

    /**
     * Garantisce che la pipeline di chiusura (sentinel + executor) venga eseguita al più
     * una volta, anche in presenza di race tra {@link #close()} e {@link #handleDisconnect()}.
     * Usare CAS invece di {@code synchronized} evita un potenziale deadlock con il lock di
     * {@link LobbyManager} quando quest'ultimo chiama {@code close()} dall'interno di
     * {@code onDisconnect}.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public RmiVirtualView(String playerName, ClientCallbackRemote callback, LobbyManager lobbyManager) {
        this.playerName = playerName;
        this.callback = callback;
        this.lobbyManager = lobbyManager;
        this.senderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rmi-sender-" + playerName);
            t.setDaemon(true);
            return t;
        });
        this.sentinel = new LivenessSentinel(
                "server-" + playerName,
                SEND_INTERVAL_MS, CHECK_INTERVAL_MS, TIMEOUT_MS,
                () -> senderExecutor.submit(() -> {
                    try { callback.onHeartbeat(); } catch (RemoteException e) { handleDisconnect(); }
                }),
                () -> lobbyManager.onDisconnect(playerName));
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
    public void activateLiveness() {
        sentinel.start();
    }

    /** Aggiorna il timestamp di liveness: da chiamare solo all'arrivo di un HeartbeatCommand. */
    void notifyInbound() {
        sentinel.notifyInbound();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        sentinel.stop();
        senderExecutor.shutdownNow();
    }

    private void handleDisconnect() {
        if (!closed.compareAndSet(false, true)) return;
        sentinel.stop();
        senderExecutor.shutdownNow();
        lobbyManager.onDisconnect(playerName);
    }
}
