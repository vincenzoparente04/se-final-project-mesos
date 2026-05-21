package network.client.socket;

import shared.message.ServerMessage;

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

    public SocketClientThread(ObjectInputStream in, SocketVirtualServer socketVirtualServer) {
        this.in = in;
        this.socketVirtualServer = socketVirtualServer;
    }

    @Override
    public void run() {
        try {
            while (true) {
                ServerMessage msg = (ServerMessage) in.readObject();
                msg.accept(this.socketVirtualServer);
            }
        } catch (EOFException | SocketException ignored) {
        } catch (IOException | ClassNotFoundException ignored) {
        } finally {
            socketVirtualServer.onDisconnected();
        }
    }
}
