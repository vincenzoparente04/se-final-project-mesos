package server;

import com.google.gson.Gson;
import controller.GameController;
import shared.dto.GameStateDto;

import java.io.PrintWriter;

/**
 * Server-side proxy for one connected client.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Receives a pre-built {@link GameStateDto} via {@link #sendState} and
 *       serialises it to JSON, sending it to the client as {@code STATE:{json}}.</li>
 *   <li>Exposes {@link #dispatch(String)} so that {@link ClientHandler} can forward
 *       raw command lines received from the client.</li>
 * </ul>
 * This class never imports or reads the GameModel directly.
 * State arrives already built from {@link GameSession}.
 */
public class VirtualView {

    private static final Gson GSON = new Gson();

    private final GameController controller;
    private final PrintWriter out;

    public VirtualView(GameController controller, PrintWriter out) {
        this.controller = controller;
        this.out = out;
    }

    // ─────────────────────────────────────────────────────────
    // State broadcasting
    // ─────────────────────────────────────────────────────────

    /**
     * Serialises the given DTO to JSON and sends it to the client.
     * Called by {@link GameSession} whenever the model notifies a change.
     */
    public void sendState(GameStateDto dto) {
        out.println("STATE:" + GSON.toJson(dto));
        if (dto.winners != null && !dto.winners.isEmpty()) {
            out.println("GAME_OVER:" + String.join(",", dto.winners));
        }
    }

    // ─────────────────────────────────────────────────────────
    // Command dispatch
    // ─────────────────────────────────────────────────────────

    /**
     * Parses one line sent by the client and routes it to the correct
     * {@link GameController} method.
     * Any exception thrown by the controller is caught and returned to
     * the client as {@code ERROR:message}.
     */
    public void dispatch(String rawMessage) {
        String[] parts = rawMessage.split(":", -1);
        String command = parts[0];

        try {
            switch (command) {
                case "CHOOSE_COLOR" -> {
                    requireArgs(parts, 3, rawMessage);
                    controller.chooseColor(parts[1], parts[2]);
                }
                case "PLACE_TOTEM" -> {
                    requireArgs(parts, 3, rawMessage);
                    controller.placeTotem(parts[1], parts[2].charAt(0));
                }
                case "DRAW_CARD" -> {
                    requireArgs(parts, 3, rawMessage);
                    controller.drawCard(parts[1], Integer.parseInt(parts[2]));
                }
                case "END_TURN" -> {
                    requireArgs(parts, 2, rawMessage);
                    controller.endTurn(parts[1]);
                }
                default -> sendError("UNKNOWN_COMMAND:" + command);
            }
        } catch (Exception e) {
            sendError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    public void sendError(String message) {
        out.println("ERROR:" + message);
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private void requireArgs(String[] parts, int expected, String rawMessage) {
        if (parts.length < expected) {
            throw new IllegalArgumentException("INVALID_COMMAND:" + rawMessage);
        }
    }
}
