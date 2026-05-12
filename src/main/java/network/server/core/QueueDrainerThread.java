package network.server.core;

import controller.GameController;
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
    private final GameController controller;
    private final BiConsumer<String, String> onError; // TODO vedi se si può levare

    /**
     * @param commandQueue the shared queue to drain
     * @param controller   the game controller that executes each command
     * @param onError      called with (playerName, errorMessage) when a
     *                     command fails; used by {@link Game} to route
     *                     the error back to the responsible player
     */
    // TODO: leva consumer se possibile (bisogna semplificare)
    public QueueDrainerThread(BlockingQueue<GameCommand> commandQueue, GameController controller, BiConsumer<String, String> onError) {
        this.commandQueue = commandQueue;
        this.controller = controller;
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
            controller.handleCommand(command); // TODO: deve fare Game.handleCommand (che diventa il nuovo controller)
        } catch (Exception e) { // TODO vedi se può essere lui stesso a notificare le view cosi evitiamo il consumer
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            onError.accept(command.getPlayerName(), message);
        }
    }
}
