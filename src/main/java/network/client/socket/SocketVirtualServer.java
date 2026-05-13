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
public class SocketVirtualServer implements VirtualServer {

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

            if (response instanceof StateMessage) {
                this.playerName = name;
                this.localState = localState;
                this.listener = listener;
                StateMessage stateMessage = (StateMessage) response;
                onStateReceived(stateMessage.state());
                return true;
            }
            if (response instanceof LobbyListMessage lobbyList) {
                this.playerName = name;
                this.localState = localState;
                this.listener = listener;
                this.bufferedLobbyList = lobbyList.lobbies();
                return true;
            }
            if (response instanceof ErrorMessage err
                    && err.message() != null
                    && err.message().startsWith("name_already_taken:")) {
                return false;
            }
            // Unexpected message during handshake: treat as a fatal failure.
            throw new IOException("Unexpected handshake response: " + response);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Name negotiation failed: " + e.getMessage(), e);
        }
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
    public void sendCreateLobby(int maxPlayers) {
        send(new CreateLobbyCommand(playerName, maxPlayers));
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
        closed.set(true);
        listener.onDisconnected();
    }
}
