package network.server.rmi;

import java.util.concurrent.BlockingQueue;

import network.server.core.PlayerEntry;
import network.server.core.VirtualView;
import shared.command.GameCommand;

/**
 * PlayerEntry is an interface that represents a player connected to the server, providing access to their name and VirtualView
 * and to register the player in the correct game command queue
 */
public class RmiPlayerEntry implements PlayerEntry {

    private final RmiVirtualView view;
    private final GameServerRemoteImpl server;

    public RmiPlayerEntry(RmiVirtualView view, GameServerRemoteImpl server) {
        this.view = view;
        this.server = server;
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
    public void setGameQueue(BlockingQueue<GameCommand> queue) {
        server.registerQueue(getName(), queue);
    }
}
