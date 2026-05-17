package network.server.rmi;

import java.util.concurrent.BlockingQueue;

import network.server.core.PlayerEntry;
import network.server.core.VirtualView;
import shared.command.GameCommand;

/**
 * RMI implementation of {@link PlayerEntry}. The game queue set by the
 * controller is forwarded to the {@link GameServerRemoteImpl} registry so
 * that incoming in-game commands from the RMI dispatch thread can be routed
 * to the right session.
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
