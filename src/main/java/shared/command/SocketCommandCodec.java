package shared.command;

/**
 * Single source of truth for the TCP socket wire-format.
 * <p>
 * Encodes any {@link GameCommand} as a colon-delimited line, e.g.
 * {@code "DRAW_CARD:Alice:42"}, by implementing {@link CommandVisitor}.
 * <p>
 * Usage:
 * <pre>
 *   String line = SocketCommandCodec.encode(new DrawCardCommand("Alice", 42));
 *   // → "DRAW_CARD:Alice:42"
 * </pre>
 * The public token constants ({@link #CHOOSE_COLOR}, {@link #PLACE_TOTEM},
 * {@link #DRAW_CARD}, {@link #END_TURN}) are used by
 * {@code server.socket.SocketCommandParser} to map incoming lines back to
 * command objects, keeping the encoding and decoding sides in sync.
 */
public class SocketCommandCodec implements CommandVisitor {

    public static final String CHOOSE_COLOR = "CHOOSE_COLOR";
    public static final String PLACE_TOTEM  = "PLACE_TOTEM";
    public static final String DRAW_CARD    = "DRAW_CARD";
    public static final String END_TURN     = "END_TURN";

    private String result;

    private SocketCommandCodec() {}

    /**
     * Encodes {@code command} as a wire-format line.
     * Never throws: the visit methods in this class do not produce exceptions.
     */
    public static String encode(GameCommand command) {
        SocketCommandCodec codec = new SocketCommandCodec();
        try {
            command.accept(codec);
        } catch (Exception e) {
            // visit methods in this class never throw
            throw new IllegalStateException("Unexpected exception encoding command", e);
        }
        return codec.result;
    }

    @Override
    public void visit(ChooseColorCommand cmd) {
        result = CHOOSE_COLOR + ":" + cmd.playerName() + ":" + cmd.color();
    }

    @Override
    public void visit(PlaceTotemCommand cmd) {
        result = PLACE_TOTEM + ":" + cmd.playerName() + ":" + cmd.tileId();
    }

    @Override
    public void visit(DrawCardCommand cmd) {
        result = DRAW_CARD + ":" + cmd.playerName() + ":" + cmd.cardId();
    }

    @Override
    public void visit(EndTurnCommand cmd) {
        result = END_TURN + ":" + cmd.playerName();
    }
}
