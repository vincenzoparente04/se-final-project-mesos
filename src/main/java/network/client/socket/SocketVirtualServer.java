package network.client.socket;

import network.client.core.LocalGameState;
import network.client.core.VirtualServer;
import network.client.core.ClientStateListener;
import shared.command.gameCommand.ChooseColorCommand;
import shared.command.ClientCommand;
import shared.command.lobbyCommand.CreateLobbyCommand;
import shared.command.gameCommand.DrawCardCommand;
import shared.command.gameCommand.EndTurnCommand;
import shared.command.lobbyCommand.JoinLobbyCommand;
import shared.command.lobbyCommand.LeaveCommand;
import shared.command.lobbyCommand.ListLobbiesCommand;
import shared.command.gameCommand.PlaceTotemCommand;
import shared.command.lobbyCommand.HeartbeatCommand;
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
 *   <li>{@link #start()} spawns the reader thread and the bidirectional liveness sentinel.</li>
 * </ol>
 */
public class SocketVirtualServer implements VirtualServer, ServerMessageVisitor {

    /** Timeout for reading the server's response to a name attempt. */
    private static final int NAME_NEGOTIATION_TIMEOUT_MS = 50_000;

    /**
     * Client-side socket liveness intervals (milliseconds). Invariant:
     * {@code TIMEOUT_MS > 2 * SEND_INTERVAL_MS}, to tolerate scheduling jitter and
     * brief delays from large application messages occupying the sender. Worst-case
     * detection time is {@code TIMEOUT_MS + CHECK_INTERVAL_MS = 20 s}.
     */
    private static final long SEND_INTERVAL_MS  = 2_000L;
    private static final long CHECK_INTERVAL_MS = 5_000L;
    private static final long TIMEOUT_MS        = 15_000L;

    /** TCP connection to the server. */
    private final Socket socket;
    /** Outbound command stream; writes are guarded by {@code synchronized (out)}. */
    private final ObjectOutputStream out;
    /** Inbound message stream, drained by the reader thread. */
    private final ObjectInputStream in;
    /** Set once the connection is closed; makes {@link #close()} idempotent. */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /** Registered player name; set during {@link #tryRegisterName}. */
    private String playerName;
    /** Local mirror of the game state, updated from server snapshots. */
    private LocalGameState localState;
    /** Listener notified of inbound server events. */
    private ClientStateListener listener;
    /** Last lobby list received, cached for the UI. */
    private List<LobbyDto> bufferedLobbyList;
    /** Bidirectional liveness watchdog; created in {@link #start()}. */
    private LivenessSentinel sentinel;

    /** Verdict of the last handshake response, read by {@link #tryRegisterName}. */
    private boolean connectionResponse;

    /**
     * Opens the TCP socket and the object streams to the server. Performs blocking
     * network I/O, so it must not run on the JavaFX Application Thread.
     *
     * @param host the server hostname or IP
     * @param port the server TCP port
     * @throws IOException if the connection or stream setup fails
     */
    public SocketVirtualServer(String host, int port) throws IOException {
        Socket s = null;
        try {
            s = new Socket(host, port);
            ObjectOutputStream objectOut = new ObjectOutputStream(s.getOutputStream());
            objectOut.flush();
            this.socket = s;
            this.out = objectOut;
            this.in = new ObjectInputStream(s.getInputStream());
        } catch (IOException e) {
            if (s != null) {
                try { s.close(); } catch (IOException ignored) {}
            }
            throw new IOException("Cannot connect to socket server at "
                    + host + ":" + port + " (" + e.getMessage() + ")", e);
        }
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
    public void visit(StateMessage msg) {
        localState.update(msg.state());
        listener.onGameStateUpdated(localState);
        connectionResponse = true;
    }

    @Override
    public void visit(ErrorMessage msg) {
        if (msg.message() != null ) {
            listener.onError(msg.message());
            if (msg.message().equals("GAME_RESUMED")) {
                connectionResponse = true;
                return;
            }
            if (msg.message().equals("connection_timeout:no_connect_message_received")) {
                System.exit(1); // exit code 1 = timeout/disconnection error
            }
        }
        connectionResponse = false;
    }

    @Override
    public void visit(GameOverMessage msg) {
        connectionResponse = true;
        listener.onGameOver(msg.winners(), msg.scoring());
    }

    @Override
    public void visit(LeaderboardMessage msg) {
        connectionResponse = true;
        listener.onLeaderboardUpdate(msg.leaderboard(), msg.rankPosition(), msg.points());
    }

    @Override
    public void visit(EventResolvedMessage msg) {
        connectionResponse = true;
        listener.onEventResolved(msg.resolution());
    }

    @Override
    public void visit(LobbyListMessage msg) {
        this.bufferedLobbyList = msg.lobbies();
        listener.onLobbyList(bufferedLobbyList);
        connectionResponse = true;
    }

    @Override
    public void visit(LobbyStateMessage msg) {
        connectionResponse = true;
        listener.onLobbyState(msg.lobby());
    }

    @Override
    public void visit(GameStartingMessage msg) {
        connectionResponse = true;
        listener.onGameStarting();
    }

    @Override
    public void visit(HeartbeatMessage msg) {
        // Isolated liveness channel: only HeartbeatMessage refreshes the client-side watchdog.
        if (sentinel != null) sentinel.notifyInbound();
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

    /**
     * Serializes and flushes a command to the server under {@code synchronized (out)}.
     * On I/O failure it triggers the client-side disconnect.
     *
     * @param cmd the command to send
     */
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

    /** Refreshes the inbound-liveness timestamp. Unused: liveness is refreshed only by {@link #visit(HeartbeatMessage)}. */
    void notifyInbound() {
        if (sentinel != null) sentinel.notifyInbound();
    }

    /** Reader-thread hook on stream end/failure: closes the connection. */
    void onDisconnected() {
        close();
    }
}
