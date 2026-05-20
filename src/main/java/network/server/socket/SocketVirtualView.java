package network.server.socket;

import network.server.core.LobbyManager;
import network.server.core.VirtualView;
import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.dto.event.EndGameScoringDto;
import shared.dto.event.EventResolutionDto;
import shared.liveness.LivenessSentinel;
import shared.message.ErrorMessage;
import shared.message.EventResolvedMessage;
import shared.message.GameOverMessage;
import shared.message.GameStartingMessage;
import shared.message.HeartbeatMessage;
import shared.message.LobbyListMessage;
import shared.message.LobbyStateMessage;
import shared.message.ServerMessage;
import shared.message.StateMessage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Server-side view of a socket client. Sole responsibility: trasmettere al
 * client i messaggi che il server le passa.
 * <p>
 * Le {@code sendXxx} sono dispatchate su un executor single-thread dedicato
 * per ogni player: in questo modo il game thread non si blocca mai sulla
 * write TCP di un client lento. La view non conosce {@code LobbyManager}; se
 * una write fallisce, si limita a chiudere il socket — il
 * {@code SocketClientHandler.run()} se ne accorge tramite EOF/SocketException
 * sul read loop e attiva la pipeline di disconnect (chiamando
 * {@code lobbyManager.onDisconnect} nel suo finally).
 */
public class SocketVirtualView implements VirtualView {

    /**
     * Periodo di invio heartbeat server→client.
     * Invariante: {@code TIMEOUT_MS > 2 * SEND_INTERVAL_MS} per tollerare il jitter di scheduling.
     */
    /**
     * Intervalli del sentinel server-side socket.
     * Invariante: {@code TIMEOUT_MS > 2 * SEND_INTERVAL_MS} per tollerare il jitter di scheduling
     * e ritardi temporanei dovuti a messaggi applicativi grandi che impegnano il sender.
     * Il detection time massimo è {@code TIMEOUT_MS + CHECK_INTERVAL_MS = 12s}.
     */
    private static final long SEND_INTERVAL_MS  = 2_000L;
    private static final long CHECK_INTERVAL_MS = 5_000L;
    private static final long TIMEOUT_MS        = 15_000L;

    private final String playerName;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ExecutorService senderExecutor;
    private volatile boolean closed = false;
    private final LivenessSentinel sentinel;

    public SocketVirtualView(String playerName, Socket socket, ObjectOutputStream out) {
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
                () -> send(new HeartbeatMessage()),
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
