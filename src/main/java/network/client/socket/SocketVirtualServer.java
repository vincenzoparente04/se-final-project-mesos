package network.client.socket;

import network.client.LocalGameState;
import network.client.VirtualServer;
import network.client.ClientStateListener;
import shared.command.ChooseColorCommand;
import shared.command.ClientCommand;
import shared.command.CreateLobbyCommand;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.JoinLobbyCommand;
import shared.command.LeaveCommand;
import shared.command.ListLobbiesCommand;
import shared.command.PlaceTotemCommand;
import shared.command.HeartbeatCommand;
import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.liveness.LivenessSentinel;
import shared.message.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Socket implementation of {@link VirtualServer} proxy.
 * <p>
 * Lifecycle:
 * <ol>
 *   <li>{@link #SocketVirtualServer(String, int)} opens the TCP socket and the
 *       Object{Input,Output}Streams. No game-layer wiring happens here.</li>
 *   <li>{@link #tryRegisterName(String, LocalGameState, ClientStateListener)}
 *       performs the name handshake. Reusable across rejections.</li>
 *   <li>{@link #start()} spawns the reader thread and il sentinel di liveness bidirezionale.</li>
 * </ol>
 */
public class SocketVirtualServer implements VirtualServer, ServerMessageHandler {

    /** Timeout for reading the server's response to a name attempt. */
    private static final int NAME_NEGOTIATION_TIMEOUT_MS = 50_000;

    /**
     * Intervalli del sentinel client-side socket.
     * Invariante: {@code TIMEOUT_MS > 2 * SEND_INTERVAL_MS} per tollerare il jitter di scheduling
     * e ritardi temporanei dovuti a messaggi applicativi grandi che impegnano il sender.
     * Il detection time massimo è {@code TIMEOUT_MS + CHECK_INTERVAL_MS = 12s}.
     */
    private static final long SEND_INTERVAL_MS  = 2_000L;
    private static final long CHECK_INTERVAL_MS = 5_000L;
    private static final long TIMEOUT_MS        = 15_000L;

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private String playerName;
    private LocalGameState localState;
    private ClientStateListener listener;
    private List<LobbyDto> bufferedLobbyList;
    private LivenessSentinel sentinel;

    private boolean connectionResponse;

    public SocketVirtualServer(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
        objectOut.flush();
        this.out = objectOut;
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public boolean tryRegisterName(String name, LocalGameState localState, ClientStateListener listener) {
        try {
            synchronized (out) {
                out.reset();
                out.writeObject(new ConnectMessage(name));
                out.flush();
            }

            socket.setSoTimeout(NAME_NEGOTIATION_TIMEOUT_MS);
            ServerMessage response = (ServerMessage) in.readObject();
            socket.setSoTimeout(0);

            this.playerName = name;
            this.localState = localState;
            this.listener = listener;

            response.accept(this);

            return connectionResponse;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Name negotiation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void handle(StateMessage msg) {
        onStateReceived(msg.state());
        connectionResponse = true;
    }

    @Override
    public void handle(ErrorMessage msg) {
        if (msg.message() != null ) {
            onErrorReceived(msg.message());
            }
        connectionResponse = false;
    }

    @Override
    public void handle(GameOverMessage msg) {
        connectionResponse = true;
        onGameOverReceived(msg.winners());
    }

    @Override
    public void handle(LobbyListMessage msg) {
        this.bufferedLobbyList = msg.lobbies();
        connectionResponse = true;
    }

    @Override
    public void handle(LobbyStateMessage msg) {
        connectionResponse = true;
        onLobbyStateReceived(msg.lobby());
    }

    @Override
    public void handle(GameStartingMessage msg) {
        connectionResponse = true;
        onGameStartingReceived();
    }

    @Override
    public void handle(HeartbeatMessage msg) {
        // No-op: la liveness è già aggiornata da notifyInbound() nel loop del SocketClientThread.
    }

    @Override
    public void start() {
        this.sentinel = new LivenessSentinel(
                "client-" + playerName,
                SEND_INTERVAL_MS, CHECK_INTERVAL_MS, TIMEOUT_MS,
                () -> send(new HeartbeatCommand(playerName)),
                this::close);
        sentinel.start();

        new Thread(new SocketClientThread(in, this), "socket-reader-" + playerName).start();

        // Replay the initial lobby list that was consumed during the handshake.
        if (bufferedLobbyList != null) {
            listener.onLobbyList(bufferedLobbyList);
            bufferedLobbyList = null;
        }
    }

    // ─── Game commands ────────────────────────────────────────

    @Override
    public void sendChooseColor(String color) {
        send(new ChooseColorCommand(playerName, color));
    }

    @Override
    public void sendPlaceTotem(char tile) {
        send(new PlaceTotemCommand(playerName, tile));
    }

    @Override
    public void sendDrawCard(int cardId) {
        send(new DrawCardCommand(playerName, cardId));
    }

    @Override
    public void sendEndTurn() {
        send(new EndTurnCommand(playerName));
    }

    // ─── Lobby commands ───────────────────────────────────────

    @Override
    public void sendCreateLobby(int playersNumber) {
        send(new CreateLobbyCommand(playerName, playersNumber));
    }

    @Override
    public void sendJoinLobby(String lobbyId) {
        send(new JoinLobbyCommand(playerName, lobbyId));
    }

    @Override
    public void sendListLobbies() {
        send(new ListLobbiesCommand(playerName));
    }

    @Override
    public void sendLeaveCommand() {
        send(new LeaveCommand(playerName));
    }

    private void send(ClientCommand cmd) {
        if (closed.get()) return;
        try {
            synchronized (out) {
                out.reset();
                out.writeObject(cmd);
                out.flush();
            }
        } catch (IOException e) {
            onDisconnected();
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (sentinel != null) sentinel.stop();
            try { socket.close(); } catch (IOException ignored) {}
            if (listener != null) listener.onDisconnected();
            System.exit(0);
        }
    }

    /** Aggiorna il timestamp di liveness: chiamato dal {@link SocketClientThread} ad ogni messaggio. */
    void notifyInbound() {
        if (sentinel != null) sentinel.notifyInbound();
    }

    // ─── Callbacks from SocketClientThread ───────────────────

    void onStateReceived(GameStateDto dto) {
        localState.update(dto);
        listener.onGameStateUpdated(localState);
    }

    void onGameOverReceived(List<String> winners) {
        listener.onGameOver(winners);
    }

    void onErrorReceived(String message) {
        listener.onError(message);
        if (message.equals("connection_timeout:no_connect_message_received")) {
            System.exit(1); // exit code 1 = timeout/disconnection error
        }
    }

    void onLobbyListReceived(List<LobbyDto> lobbies) {
        listener.onLobbyList(lobbies);
    }

    void onLobbyStateReceived(LobbyDto lobby) {
        listener.onLobbyState(lobby);
    }

    void onGameStartingReceived() {
        listener.onGameStarting();
    }

    void onDisconnected() {
        close();
    }
}
