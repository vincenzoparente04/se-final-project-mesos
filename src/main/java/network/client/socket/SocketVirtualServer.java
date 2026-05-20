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
import shared.message.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Socket implementation of {@link VirtualServer} proxy.
 * <p>
 * Lifecycle:
 * <ol>
 *   <li>{@link #SocketVirtualServer(String, int)} opens the TCP socket and the
 *       Object{Input,Output}Streams. No game-layer wiring happens here.</li>
 *   <li>{@link #tryRegisterName(String, LocalGameState, ClientStateListener)}
 *       performs the name handshake. Reusable across rejections.</li>
 *   <li>{@link #start()} spawns the reader thread and the heartbeat scheduler.</li>
 * </ol>
 */
public class SocketVirtualServer implements VirtualServer, ServerMessageVisitor {

    /** Timeout for reading the server's response to a name attempt. */
    private static final int NAME_NEGOTIATION_TIMEOUT_MS = 5_000;

    /** Periodo di invio heartbeat: deve essere < del timeout server (6s). */
    private static final long HEARTBEAT_INTERVAL_MS = 2_000L;

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private String playerName;
    private LocalGameState localState;
    private ClientStateListener listener;
    private List<LobbyDto> bufferedLobbyList;
    private ScheduledExecutorService heartbeatScheduler;

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
    public void start() {
        new Thread(new SocketClientThread(in, this), "socket-reader-" + playerName).start();

        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "heartbeat-sender-" + playerName);
            t.setDaemon(true);
            return t;
        });
        this.heartbeatScheduler.scheduleAtFixedRate(
                () -> send(new HeartbeatCommand(playerName)),
                HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
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
            if (heartbeatScheduler != null) heartbeatScheduler.shutdownNow();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    void onDisconnected() {
        closed.set(true);
        listener.onDisconnected();
    }
}
