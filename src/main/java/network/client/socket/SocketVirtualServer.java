package network.client.socket;

import network.client.LocalGameState;
import network.client.VirtualServer;
import network.client.clientStateListener.ClientStateListener;
import shared.command.ChooseColorCommand;
import shared.command.ClientCommand;
import shared.command.CreateLobbyCommand;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.JoinLobbyCommand;
import shared.command.ListLobbiesCommand;
import shared.command.PlaceTotemCommand;
import shared.dto.GameStateDto;
import shared.dto.LobbyDto;
import shared.message.ConnectMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketVirtualServer implements VirtualServer {

    private final String playerName;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final LocalGameState localState;
    private final ClientStateListener listener;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public SocketVirtualServer(String host, int port, String playerName,
                               LocalGameState localState, ClientStateListener listener)
            throws IOException {
        this.playerName = playerName;
        this.localState = localState;
        this.listener = listener;

        this.socket = new Socket(host, port);
        ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
        objectOut.flush();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        objectOut.writeObject(new ConnectMessage(playerName));
        objectOut.flush();
        this.out = objectOut;

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
