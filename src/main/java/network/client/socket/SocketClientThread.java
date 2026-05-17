package network.client.socket;

import shared.message.ErrorMessage;
import shared.message.GameOverMessage;
import shared.message.GameStartingMessage;
import shared.message.HeartbeatMessage;
import shared.message.LobbyListMessage;
import shared.message.LobbyStateMessage;
import shared.message.ServerMessage;
import shared.message.ServerMessageHandler;
import shared.message.StateMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;

/**
 * Background thread that continuously reads objects from the socket's ObjectInputStream and dispatches them to the SocketVirtualServer.
 */
public class SocketClientThread implements Runnable {

    private final ObjectInputStream in;
    private final SocketVirtualServer socketVirtualServer;

    private final ServerMessageHandler handler = new ServerMessageHandler() {
        @Override public void handle(StateMessage m)       { socketVirtualServer.onStateReceived(m.state()); }
        @Override public void handle(ErrorMessage m)       { socketVirtualServer.onErrorReceived(m.message()); }
        @Override public void handle(GameOverMessage m)    { socketVirtualServer.onGameOverReceived(m.winners()); }
        @Override public void handle(LobbyListMessage m)   { socketVirtualServer.onLobbyListReceived(m.lobbies()); }
        @Override public void handle(LobbyStateMessage m)  { socketVirtualServer.onLobbyStateReceived(m.lobby()); }
        @Override public void handle(GameStartingMessage m){ socketVirtualServer.onGameStartingReceived(); }
        // Canale di liveness isolato: solo HeartbeatMessage aggiorna il watchdog client-side.
        @Override public void handle(HeartbeatMessage m)   { socketVirtualServer.notifyInbound(); }
    };

    public SocketClientThread(ObjectInputStream in, SocketVirtualServer socketVirtualServer) {
        this.in = in;
        this.socketVirtualServer = socketVirtualServer;
    }

    @Override
    public void run() {
        try {
            while (true) {
                ServerMessage msg = (ServerMessage) in.readObject();
                msg.accept(handler);
            }
        } catch (EOFException | SocketException ignored) {
        } catch (IOException | ClassNotFoundException ignored) {
        } finally {
            socketVirtualServer.onDisconnected();
        }
    }
}
