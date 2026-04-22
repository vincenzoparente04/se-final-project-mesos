package server.socket;

import shared.command.ChooseColorCommand;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.GameCommand;
import shared.command.PlaceTotemCommand;
import shared.command.SocketCommandCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Parses raw socket lines into {@link GameCommand} objects.
 * <p>
 * Command tokens are mapped to factory lambdas in a {@link Map}, so no
 * {@code instanceof} checks or {@code switch} statements are needed.
 * An unknown token raises an {@link IllegalArgumentException} that
 * {@link SocketClientHandler} translates into an {@code ERROR} message back
 * to the client.
 */
public class SocketCommandParser {

    private final Map<String, Function<String[], GameCommand>> factories;

    public SocketCommandParser() {
        factories = new HashMap<>();
        factories.put(SocketCommandCodec.CHOOSE_COLOR, parts -> {
            requireParts(parts, 3);
            return new ChooseColorCommand(parts[1], parts[2]);
        });
        factories.put(SocketCommandCodec.PLACE_TOTEM, parts -> {
            requireParts(parts, 3);
            return new PlaceTotemCommand(parts[1], parts[2].charAt(0));
        });
        factories.put(SocketCommandCodec.DRAW_CARD, parts -> {
            requireParts(parts, 3);
            return new DrawCardCommand(parts[1], Integer.parseInt(parts[2]));
        });
        factories.put(SocketCommandCodec.END_TURN, parts -> {
            requireParts(parts, 2);
            return new EndTurnCommand(parts[1]);
        });
    }

    /**
     * Parses a raw socket line such as {@code "DRAW_CARD:Alice:42"} into
     * the corresponding {@link GameCommand}.
     *
     * @param rawLine the colon-delimited command line
     * @return the parsed command
     * @throws IllegalArgumentException if the token is unknown or the line
     *                                  has too few parts
     */
    public GameCommand parse(String rawLine) {
        String[] parts = rawLine.split(":", -1);
        Function<String[], GameCommand> factory = factories.get(parts[0]);
        if (factory == null) {
            throw new IllegalArgumentException("UNKNOWN_COMMAND:" + parts[0]);
        }
        return factory.apply(parts);
    }

    private static void requireParts(String[] parts, int minimum) {
        if (parts.length < minimum) {
            throw new IllegalArgumentException(
                    "INVALID_COMMAND:" + String.join(":", parts));
        }
    }
}
