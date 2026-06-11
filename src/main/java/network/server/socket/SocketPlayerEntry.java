package network.server.socket;

import java.io.ObjectInputStream;
import java.util.concurrent.BlockingQueue;

import network.server.core.PlayerEntry;
import network.server.core.VirtualView;
import shared.command.gameCommand.GameCommand;

/**
 * Socket implementation of {@link PlayerEntry}. Bundles the player's name, inbound
 * stream, {@link SocketVirtualView} and {@link SocketClientHandler}; wiring the
 * game queue is delegated to the handler so the reader thread can route in-game
 * commands once the player joins a session.
 */
public class SocketPlayerEntry implements PlayerEntry {

    /** The player's server-wide name. */
    private final String name;
    /** Inbound stream, exposed via {@link #getIn()} for session wiring. */
    private final ObjectInputStream in;
    /** Outbound view for this player. */
    private final SocketVirtualView view;
    /** Reader/dispatcher, target of {@link #setGameQueue}. */
    private final SocketClientHandler handler;

    /**
     * @param name the player's server-wide name
     * @param in the inbound command stream
     * @param view the outbound view for this player
     * @param handler the reader/dispatcher to wire the game queue into
     */
    public SocketPlayerEntry(String name, ObjectInputStream in, SocketVirtualView view, SocketClientHandler handler) {
        this.name = name;
        this.in = in;
        this.view = view;
        this.handler = handler;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public VirtualView getView() {
        return view;
    }

    @Override
    public void setGameQueue(BlockingQueue<GameCommand> queue) {
        handler.setGameQueue(queue);
    }

    /** @return the inbound command stream for this player */
    public ObjectInputStream getIn() {
        return in;
    }
}
