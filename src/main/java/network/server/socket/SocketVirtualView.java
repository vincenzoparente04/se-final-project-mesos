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

public class SocketVirtualView implements VirtualView {

    private final String playerName;
    private final Socket socket;
    private final ObjectOutputStream out;

    public SocketVirtualView(String playerName, Socket socket, ObjectOutputStream out) {
        this.playerName = playerName;
        this.socket = socket;
        this.out = out;
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
    public void close() {
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
