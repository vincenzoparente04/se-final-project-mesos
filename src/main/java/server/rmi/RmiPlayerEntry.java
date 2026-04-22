package server.rmi;

import server.core.PlayerEntry;
import server.core.VirtualView;
import shared.command.GameCommand;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * {@link PlayerEntry} for an RMI connection.
 * <p>
 * The {@link RmiVirtualView} is created when the player joins the lobby and
 * is immediately available for sending {@code WAITING} notifications.
 * <p>
 * {@link #activate} installs the real disconnect handler on the view
 * (until game start the view uses a no-op) and returns immediately — no
 * read thread is needed because the RMI framework routes incoming
 * {@code submitCommand} calls from the client to the server directly.
 */
public class RmiPlayerEntry implements PlayerEntry {

    private final RmiVirtualView view;

    public RmiPlayerEntry(RmiVirtualView view) {
        this.view = view;
    }

    @Override
    public String getName() {
        return view.getPlayerName();
    }

    @Override
    public VirtualView getView() {
        return view;
    }

    @Override
    public void activate(BlockingQueue<GameCommand> commandQueue,
                         Consumer<String> onDisconnect) {
        view.setOnDisconnect(onDisconnect);
    }
}
