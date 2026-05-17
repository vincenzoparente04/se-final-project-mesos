package network.server.socket;

import network.server.core.VirtualView;
import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.message.ErrorMessage;
import shared.message.GameOverMessage;
import shared.message.GameStartingMessage;
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

    private final String playerName;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ExecutorService senderExecutor;
    private volatile boolean closed = false;

    public SocketVirtualView(String playerName, Socket socket, ObjectOutputStream out) {
        this.playerName = playerName;
        this.socket = socket;
        this.out = out;
        this.senderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "socket-sender-" + playerName);
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void sendState(GameStateDto dto) {
        if (closed) return;
        senderExecutor.submit(() -> rawSend(new StateMessage(dto)));
        if (dto.winners != null && !dto.winners.isEmpty()) {
            senderExecutor.submit(() -> rawSend(new GameOverMessage(dto.winners)));
        }
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
    public synchronized void close() {
        if (closed) return;
        closed = true;
        senderExecutor.shutdown();
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
