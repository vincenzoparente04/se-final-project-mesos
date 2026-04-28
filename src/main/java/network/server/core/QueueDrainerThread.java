package network.server.core;

import shared.command.CommandVisitor;
import shared.command.GameCommand;

import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;

/**
 * Dedicated thread that drains the central command queue and executes each
 * command sequentially on the game controller.
 * <p>
 * Sequential processing guarantees that the controller (and the underlying
 * model) is never accessed concurrently from the command path — regardless
 * of how many socket/RMI threads produced the commands.
 * <p>
 * Errors thrown during command execution are forwarded to the
 * {@code onError} callback instead of crashing the thread, so a single bad
 * command never terminates the game loop.
 */
public class QueueDrainerThread implements Runnable {

    private final BlockingQueue<GameCommand> commandQueue;
    private final CommandVisitor executor;
    private final BiConsumer<String, String> onError;

    /**
     * @param commandQueue the shared queue to drain
     * @param executor     visitor that maps each command to a controller call
     * @param onError      called with (playerName, errorMessage) when a
     *                     command fails; used by {@link Game} to route
     *                     the error back to the responsible player
     */
    public QueueDrainerThread(BlockingQueue<GameCommand> commandQueue,
                      CommandVisitor executor,
                      BiConsumer<String, String> onError) {
        this.commandQueue = commandQueue;
        this.executor = executor;
        this.onError = onError;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                GameCommand command = commandQueue.take();
                execute(command);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void execute(GameCommand command) {
        try {
            command.accept(executor);
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            onError.accept(command.getPlayerName(), message);
        }
    }
}
