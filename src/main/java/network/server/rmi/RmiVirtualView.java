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
 * Server-side view of an RMI client. It forwards the server's messages to the
 * client's exported {@link ClientCallbackRemote} and starts the liveness sentinel.
 * <p>
 * Every {@code sendXxx} is dispatched onto a dedicated single-thread executor per
 * player, which issues the (synchronous, blocking) RMI callbacks one at a time and
 * in order — so the client never receives overlapping callbacks. A failed callback
 * ({@link RemoteException}) triggers the disconnect pipeline.
 */
public class RmiVirtualView implements VirtualView {

    /**
     * Server-side RMI liveness intervals (milliseconds). Invariant:
     * {@code TIMEOUT_MS > 2 * SEND_INTERVAL_MS}, to tolerate scheduling jitter and
     * brief delays from large application messages occupying the sender. Worst-case
     * detection time is {@code TIMEOUT_MS + CHECK_INTERVAL_MS = 20 s}.
     */
    private static final long SEND_INTERVAL_MS  = 2_000L;
    /** How often the sentinel checks for a missing inbound heartbeat, in milliseconds. */
    private static final long CHECK_INTERVAL_MS = 5_000L;
    /** Inbound-heartbeat timeout; if exceeded the connection is declared dead, in milliseconds. */
    private static final long TIMEOUT_MS        = 15_000L;

    /** Server-wide name of the player this view serves. */
    private final String playerName;
    /** The client's exported callback, target of every {@code sendXxx}. */
    private final ClientCallbackRemote callback;
    /** Registry notified on a liveness timeout or a failed callback. */
    private final LobbyManager lobbyManager;
    /** Single-thread executor that serialises all callbacks for this client. */
    private final ExecutorService senderExecutor;
    /** Bidirectional liveness watchdog (heartbeat sender + inbound timeout). */
    private final LivenessSentinel sentinel;

    /**
     * Ensures the shutdown pipeline (sentinel + executor) runs at most once, even
     * under a race between {@link #close()} and {@link #handleDisconnect()}. A CAS is
     * used instead of {@code synchronized} so a callback thread never blocks on a
     * monitor while completing the teardown.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * @param playerName the player's server-wide name
     * @param callback the client's exported callback for server→client messages
     * @param lobbyManager the registry to notify on a liveness timeout or callback failure
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
     * Tears down the view after a failed callback: stops the sentinel, notifies the
     * {@link LobbyManager} of the disconnect and shuts the sender down. Runs at most
     * once (guarded by {@link #closed}).
     */
    private void handleDisconnect() {
        if (!closed.compareAndSet(false, true)) return;
        sentinel.stop();
        lobbyManager.onDisconnect(playerName);
        senderExecutor.shutdownNow();
    }
}
