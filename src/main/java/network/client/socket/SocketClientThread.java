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

    /** Inbound object stream from the server. */
    private final ObjectInputStream in;
    /** Proxy whose visitor handles each decoded {@link ServerMessage}. */
    private final SocketVirtualServer socketVirtualServer;

    /**
     * @param in the inbound object stream from the server
     * @param socketVirtualServer the proxy that dispatches each decoded message
     */
    public SocketClientThread(ObjectInputStream in, SocketVirtualServer socketVirtualServer) {
        this.in = in;
        this.socketVirtualServer = socketVirtualServer;
    }

    /**
     * Read loop: decode each {@link ServerMessage} and dispatch it to the proxy's
     * visitor, in order, on this thread. On stream end or failure it leaves the loop
     * and triggers the client-side disconnect in the {@code finally} block.
     */
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
