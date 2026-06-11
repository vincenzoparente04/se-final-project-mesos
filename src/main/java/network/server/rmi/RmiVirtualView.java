package network.server.rmi;

import database.ScoreRecord;
import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;
import shared.liveness.LivenessSentinel;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import network.client.rmi.ClientCallbackRemote;
import network.server.core.LobbyManager;
import network.server.core.VirtualView;

/**
 * Server-side view of an RMI client. Sole responsibility: delivering server
 * messages to the client by invoking methods on the {@link ClientCallbackRemote}
 * stub.
 *
 * All {@code sendXxx} calls are dispatched on a dedicated single-thread
 * executor per player, so the game thread never blocks on a slow RMI call.
 * On any {@link java.rmi.RemoteException}, {@link #handleDisconnect()} is
 * called, which triggers the lobby-manager disconnect pipeline exactly once,
 * guarded by a CAS on {@link #closed} to avoid races between {@link #close()}
 * and {@link #handleDisconnect()}.
 *
 * A {@link LivenessSentinel} is started by {@link #activateLiveness()} and
 * runs a bidirectional heartbeat: it invokes {@link #sendHeartbeat()} every
 * {@value #SEND_INTERVAL_MS} ms and declares the connection dead if no
 * inbound heartbeat arrives within {@value #TIMEOUT_MS} ms.
 */
public class RmiVirtualView implements VirtualView {

    /** Heartbeat send interval (server to client), in milliseconds. */
    private static final long SEND_INTERVAL_MS  = 2_000L;
    /** How often the sentinel checks for a missing inbound heartbeat, in milliseconds. */
    private static final long CHECK_INTERVAL_MS = 5_000L;
    /** Inbound-heartbeat timeout; if exceeded the connection is declared dead, in milliseconds. */
    private static final long TIMEOUT_MS        = 15_000L;

    private final String playerName;
    private final ClientCallbackRemote callback;
    private final LobbyManager lobbyManager;
    private final ExecutorService senderExecutor;
    private final LivenessSentinel sentinel;

    /**
     * Ensures the shutdown pipeline (sentinel + executor) runs at most once,
     * even when {@link #close()} and {@link #handleDisconnect()} race.
     * CAS avoids a potential deadlock with {@link LobbyManager}'s internal
     * lock, which may call {@link #close()} from within {@code onDisconnect}.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates the view, sets up the single-thread sender executor and
     * initialises the {@link LivenessSentinel} (not started yet;
     * call {@link #activateLiveness()} when the connection is ready).
     *
     * @param playerName   the name that identifies this player on the server
     * @param callback     the RMI stub used to deliver messages to the client
     * @param lobbyManager the lobby manager to notify on timeout or disconnect
     */
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
                this::sendHeartbeat,
                () -> lobbyManager.onDisconnect(playerName));
    }

    @Override
    public void sendState(GameStateDto dto) {
        if (closed.get()) return;
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
        if (closed.get()) return;
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
        if (closed.get()) return;
        senderExecutor.submit(() -> {
            try {
                callback.onGameOver(winners, scoring);
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public void sendLeaderboard(List<ScoreRecord> leaderboard, int rankPosition, int points) {
        if (closed.get()) return;
        senderExecutor.submit(() -> {
            try {
                callback.onLeaderboardUpdate(leaderboard, rankPosition, points);
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
    public void sendHeartbeat() {
        if (closed.get()) return;
        senderExecutor.submit(() -> {
            try {
                callback.onHeartbeat();
            } catch (RemoteException e) {
                handleDisconnect();
            }
        });
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Starts the {@link LivenessSentinel}, enabling the bidirectional heartbeat.
     * Must be called once the RMI registration is complete.
     */
    @Override
    public void activateLiveness() {
        sentinel.start();
    }

    /**
     * Updates the liveness timestamp. Must be called only upon receiving
     * a {@link shared.command.lobbyCommand.HeartbeatCommand} from the client.
     */
    @Override
    public void notifyInbound() {
        sentinel.notifyInbound();
    }

    /**
     * Shuts down this view cleanly: stops the sentinel and terminates the
     * sender executor. Idempotent — safe to call multiple times.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        sentinel.stop();
        senderExecutor.shutdownNow();
    }

    /**
     * Called when a {@link java.rmi.RemoteException} is thrown during a send.
     * Stops the sentinel, notifies the lobby manager of the disconnection, and
     * shuts down the sender executor. Idempotent via CAS on {@link #closed}.
     */
    private void handleDisconnect() {
        if (!closed.compareAndSet(false, true)) return;
        sentinel.stop();
        lobbyManager.onDisconnect(playerName);
        senderExecutor.shutdownNow();
    }
}
