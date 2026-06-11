package network.server.socket;

import database.ScoreRecord;
import network.server.core.LobbyManager;
import network.server.core.VirtualView;
import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;
import shared.liveness.LivenessSentinel;
import shared.message.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Server-side view of a socket client. Its sole responsibility is to transmit to
 * the client the messages the server hands it.
 * <p>
 * Every {@code sendXxx} is dispatched onto a dedicated single-thread executor per
 * player, so the game-thread never blocks on the TCP write of a slow client, and
 * outbound messages to a given client are never interleaved. The view does not
 * know the {@code LobbyManager}; if a write fails it just closes the socket, and
 * {@code SocketClientHandler.run()} notices it via EOF/SocketException on its read
 * loop and triggers the disconnect pipeline (calling {@code lobbyManager.onDisconnect}
 * in its {@code finally}).
 */
public class SocketVirtualView implements VirtualView {

    /**
     * Server-side socket liveness intervals (milliseconds). Invariant:
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
    /** The client's socket; closed on shutdown or on a write failure. */
    private final Socket socket;
    /** Outbound object stream; written only on the sender thread. */
    private final ObjectOutputStream out;
    /** Single-thread executor that serialises all outbound writes for this client. */
    private final ExecutorService senderExecutor;
    /** Set once the view is closed; gates every {@code sendXxx} and {@link #rawSend}. */
    private volatile boolean closed = false;
    /** Bidirectional liveness watchdog (heartbeat sender + inbound timeout). */
    private final LivenessSentinel sentinel;

    /**
     * Creates the view, sets up the single-thread sender executor and
     * initialises the {@link LivenessSentinel} (not started yet;
     * call {@link #activateLiveness()} when the connection is ready).
     *
     * @param playerName   the name that identifies this player on the server
     * @param socket       the underlying TCP socket (closed on write failure or {@link #close()})
     * @param out          the object output stream to write messages to
     * @param lobbyManager the lobby manager to notify when a timeout is detected
     */
    public SocketVirtualView(String playerName, Socket socket, ObjectOutputStream out, LobbyManager lobbyManager) {
        this.playerName = playerName;
        this.socket = socket;
        this.out = out;
        this.senderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "socket-sender-" + playerName);
            t.setDaemon(true);
            return t;
        });

        this.sentinel = new LivenessSentinel(
                "server-" + playerName,
                SEND_INTERVAL_MS, CHECK_INTERVAL_MS, TIMEOUT_MS,
                () -> sendHeartbeat(),
                () -> lobbyManager.onDisconnect(playerName));
    }

    @Override
    public void sendState(GameStateDto dto) {
        if (closed) return;
        senderExecutor.submit(() -> rawSend(new StateMessage(dto)));
        // Game-over is no longer auto-emitted here: the phase/controller
        // calls sendGameOver(...) explicitly so that the winners ship with
        // the scoring breakdown.
    }

    @Override
    public void sendEventResolved(EventResolutionDto resolution) {
        if (closed) return;
        senderExecutor.submit(() -> rawSend(new EventResolvedMessage(resolution)));
    }

    @Override
    public void sendGameOver(List<String> winners, EndGameScoringDto scoring) {
        if (closed) return;
        senderExecutor.submit(() -> rawSend(new GameOverMessage(winners, scoring)));
    }

    @Override
    public void sendLeaderboard(List<ScoreRecord> leaderboard, int rankPosition, int points) {
        if (closed) return;
        senderExecutor.submit(() -> rawSend(new LeaderboardMessage(leaderboard, rankPosition, points)));
    }

    @Override
    public void sendError(String message) {
        if (closed) return;
        senderExecutor.submit(() -> rawSend(new ErrorMessage(message)));
    }

    @Override
    public void sendLobbyList(List<LobbyDto> lobbies) {
        if (closed) return;
        senderExecutor.submit(() -> rawSend(new LobbyListMessage(lobbies)));
    }

    @Override
    public void sendLobbyState(LobbyDto lobby) {
        if (closed) return;
        senderExecutor.submit(() -> rawSend(new LobbyStateMessage(lobby)));
    }

    @Override
    public void sendGameStarting() {
        if (closed) return;
        senderExecutor.submit(() -> rawSend(new GameStartingMessage()));
    }

    @Override
    public void sendHeartbeat() {
        if (closed) return;
        senderExecutor.submit(() -> rawSend(new HeartbeatMessage()));
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Starts the {@link LivenessSentinel}, enabling the bidirectional heartbeat.
     * Must be called once the connection handshake is complete.
     */
    @Override
    public void activateLiveness() {
        sentinel.start();
    }

    /** Refreshes the inbound-liveness timestamp; called only on arrival of a heartbeat. */
    /**
     * Updates the liveness timestamp. Must be called only upon receiving
     * a {@link shared.command.lobbyCommand.HeartbeatCommand} from the client.
     */
    @Override
    public void notifyInbound() {
        sentinel.notifyInbound();
    }

    /**
     * Shuts down this view cleanly: stops accepting new messages, waits up
     * to 500 ms for in-flight sends to complete, stops the sentinel, and
     * closes the underlying socket.
     */
    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        senderExecutor.shutdown();
        sentinel.stop();
        try {
            if (!senderExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                senderExecutor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            senderExecutor.shutdownNow();
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    /**
     * Writes a message directly to the output stream on the caller's thread
     * (always the sender executor). Resets the stream before each write to
     * prevent stale object-graph caching. On any {@link IOException}, marks
     * the view as closed, shuts down the executor and closes the socket so
     * that {@link SocketClientHandler}'s read loop detects the failure.
     *
     * @param msg the message to serialise and send
     */
    private void rawSend(ServerMessage msg) {
        if (closed) return;
        try {
            out.reset();
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            // Send failed: close the socket so SocketClientHandler.run() picks
            // up the EOF on the read side and runs its usual disconnect path.
            closed = true;
            senderExecutor.shutdownNow();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
