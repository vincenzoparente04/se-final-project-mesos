package server.socket;

import server.core.VirtualView;
import shared.command.ChooseColorCommand;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.GameCommand;
import shared.command.PlaceTotemCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Dedicated reading thread for one TCP-connected client.
 * <p>
 * Reads lines from the socket in a blocking loop, and places the resulting {@link GameCommand}
 * on the shared {@link BlockingQueue} for sequential processing by
 * {@link server.core.GameThread}.
 * <p>
 * Parse errors (unknown command, malformed arguments) are returned to the
 * client as {@code ERROR:} messages via the player's {@link VirtualView}.
 * On disconnect or I/O error the {@code onDisconnect} callback is invoked
 * with the player's name so that {@link server.core.GameSession} can notify
 * the remaining players.
 */
public class SocketClientHandler implements Runnable {

    private final VirtualView virtualView;
    private final BufferedReader in;
    private final BlockingQueue<GameCommand> commandQueue;
    private final Consumer<String> onDisconnect;

    public SocketClientHandler(VirtualView virtualView,
                               BufferedReader in,
                               BlockingQueue<GameCommand> commandQueue,
                               Consumer<String> onDisconnect) {
        this.virtualView = virtualView;
        this.in = in;
        this.commandQueue = commandQueue;
        this.onDisconnect = onDisconnect;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleLine(line.trim());
            }
        } catch (IOException ignored) {
            // client closed connection abruptly
        } finally {
            onDisconnect.accept(virtualView.getPlayerName());
        }
    }

    private void handleLine(String line) {
        try {
            GameCommand command = parseStringToCommand(line);
            commandQueue.put(command);
        } catch (IllegalArgumentException e) {
            virtualView.sendError(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Translates a raw string received from the socket into a {@link GameCommand}.
     * Uses a switch statement to parse and generate the appropriate command type.
     * The command is then queued for processing.
     *
     * @param rawLine the raw command string from the client (colon-delimited)
     * @return a {@link GameCommand} object parsed from the input string
     * @throws IllegalArgumentException if the string cannot be parsed into a valid command
     */
    private GameCommand parseStringToCommand(String rawLine) {
        String[] parts = rawLine.split(":", -1);
        String commandToken = parts[0];

        return switch (commandToken) {
            case "CHOOSE_COLOR" -> {
                validateParts(parts, 3);
                yield new ChooseColorCommand(parts[1], parts[2]);
            }
            case "PLACE_TOTEM" -> {
                validateParts(parts, 3);
                yield new PlaceTotemCommand(parts[1], parts[2].charAt(0));
            }
            case "DRAW_CARD" -> {
                validateParts(parts, 3);
                yield new DrawCardCommand(parts[1], Integer.parseInt(parts[2]));
            }
            case "END_TURN" -> {
                validateParts(parts, 2);
                yield new EndTurnCommand(parts[1]);
            }
            default -> throw new IllegalArgumentException("UNKNOWN_COMMAND:" + commandToken);
        };
    }

    /**
     * Validates that the parsed parts array has at least the minimum required elements.
     */
    private static void validateParts(String[] parts, int minimum) {
        if (parts.length < minimum) {
            throw new IllegalArgumentException(
                    "INVALID_COMMAND:" + String.join(":", parts));
        }
    }
}
