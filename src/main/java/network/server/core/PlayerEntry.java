package network.server.core;

import java.util.concurrent.BlockingQueue;

import shared.command.GameCommand;

public interface PlayerEntry {

    String getName();

    VirtualView getView();

    default void setGameQueue(BlockingQueue<GameCommand> queue) {}
}
