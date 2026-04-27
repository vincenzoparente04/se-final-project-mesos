package server.socket;

import server.core.PlayerEntry;
import server.core.VirtualView;
import shared.command.GameCommand;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * {@link PlayerEntry} for a TCP socket connection.
 * <p>
 * Holds the {@link PlayerConnection} (I/O streams + socket) and the
 * pre-created {@link SocketVirtualView}.  On {@link #activate}, a
 * {@link SocketClientHandler} thread is launched that reads lines from the
 * socket and parses them into {@link GameCommand}.
 */
public class SocketPlayerEntry implements PlayerEntry {

    private final PlayerConnection connection;
    private final SocketVirtualView view;

    public SocketPlayerEntry(PlayerConnection connection,
                             SocketVirtualView view) {
        this.connection = connection;
        this.view = view;
    }

    @Override
    public String getName() {
        return connection.name();
    }

    @Override
    public VirtualView getView() {
        return view;
    }

    @Override
    public void activate(BlockingQueue<GameCommand> commandQueue,
                         Consumer<String> onDisconnect) {
        SocketClientHandler handler = new SocketClientHandler(
                view, connection.in(), commandQueue, onDisconnect);
        Thread t = new Thread(handler, "client-" + connection.name());
        t.setDaemon(true);
        t.start();
    }
}
