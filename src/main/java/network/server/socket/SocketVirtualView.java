package network.server.socket;

import network.server.core.LobbyManager;
import network.server.core.VirtualView;
import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.liveness.LivenessSentinel;
import shared.message.ErrorMessage;
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
    private static final long CHECK_INTERVAL_MS = 2_000L;
    private static final long TIMEOUT_MS        = 10_000L;

    private final String playerName;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final LivenessSentinel sentinel;

    public SocketVirtualView(String playerName, Socket socket, ObjectOutputStream out,
                             LobbyManager lobbyManager) {
        this.playerName = playerName;
        this.socket = socket;
        this.out = out;
        this.sentinel = new LivenessSentinel(
                "server-" + playerName,
                SEND_INTERVAL_MS, CHECK_INTERVAL_MS, TIMEOUT_MS,
                () -> send(new HeartbeatMessage()),
                () -> lobbyManager.onDisconnect(playerName));
    }

    @Override
    public void sendState(GameStateDto dto) {
        synchronized (out) {
            send(new StateMessage(dto));
            if (dto.winners != null && !dto.winners.isEmpty()) {
                send(new GameOverMessage(dto.winners));
            }
        }
    }

    @Override
    public void sendError(String message) {
        send(new ErrorMessage(message));
    }

    @Override
    public void sendLobbyList(List<LobbyDto> lobbies) {
        send(new LobbyListMessage(lobbies));
    }

    @Override
    public void sendLobbyState(LobbyDto lobby) {
        send(new LobbyStateMessage(lobby));
    }

    @Override
    public void sendGameStarting() {
        send(new GameStartingMessage());
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
        sentinel.stop();
        try { socket.close(); } catch (IOException ignored) {}
    }

    private void send(ServerMessage msg) {
        try {
            synchronized (out) {
                out.reset();
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException ignored) {}
    }
}
