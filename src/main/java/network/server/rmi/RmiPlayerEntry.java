package network.server.rmi;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import network.server.core.DisconnectListener;
import network.server.core.PlayerEntry;
import network.server.core.VirtualView;
import shared.command.GameCommand;

public class RmiPlayerEntry implements PlayerEntry {

    private final RmiVirtualView view;
    private final Consumer<BlockingQueue<GameCommand>> onQueue;

    public RmiPlayerEntry(RmiVirtualView view, Consumer<BlockingQueue<GameCommand>> onQueue,
                          DisconnectListener disconnectListener) {
        this.view = view;
        this.onQueue = onQueue;
        view.setOnDisconnect(disconnectListener);
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
        onQueue.accept(queue);
    }
}
