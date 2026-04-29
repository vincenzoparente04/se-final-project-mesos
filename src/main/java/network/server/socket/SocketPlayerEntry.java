package network.server.socket;

import java.io.ObjectInputStream;
import java.util.concurrent.BlockingQueue;

import network.server.core.PlayerEntry;
import network.server.core.VirtualView;
import shared.command.GameCommand;

public class SocketPlayerEntry implements PlayerEntry {

    private final String name;
    private final ObjectInputStream in;
    private final SocketVirtualView view;
    private final SocketClientHandler handler;

    public SocketPlayerEntry(String name, ObjectInputStream in, SocketVirtualView view,
                             SocketClientHandler handler) {
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

    public ObjectInputStream getIn() {
        return in;
    }
}
