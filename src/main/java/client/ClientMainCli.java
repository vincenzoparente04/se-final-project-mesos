package client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Minimal command-line client for playing Mesos without JavaFX.
 * <p>
 * Usage: {@code java client.ClientMainCli <host> <port> <playerName>}
 * <p>
 * The player types commands (without the playerName prefix) and the client
 * prepends it automatically. State updates are printed as compact JSON to stdout.
 * <p>
 * Supported input commands:
 * <pre>
 *   color RED          →  CHOOSE_COLOR:name:RED
 *   totem A            →  PLACE_TOTEM:name:A
 *   draw 42            →  DRAW_CARD:name:42
 *   end                →  END_TURN:name
 *   quit               →  closes the connection
 * </pre>
 */
public class ClientMainCli {

    /** Objects/arrays whose compact form fits within this limit stay on one line. */
    private static final int COMPACT_THRESHOLD = 100;
    private static final String INDENT = "  ";

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: ClientMainCli <host> <port> <playerName>");
            return;
        }
        String host       = args[0];
        int port          = Integer.parseInt(args[1]);
        String playerName = args[2];

        Socket socket = new Socket(host, port);
        PrintWriter out   = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("CONNECT:" + playerName);
        System.out.println("Connected as " + playerName + ". Waiting for other players...");
        System.out.println("Commands: color <COLOR> | totem <LETTER> | draw <ID> | end | quit");

        // Background thread — prints everything received from the server
        Thread reader = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("\n" + format(line));
                    System.out.print("> ");
                }
            } catch (IOException ignored) {}
        });
        reader.setDaemon(true);
        reader.start();

        // Main thread — reads stdin commands and sends them to the server
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String input;
        while ((input = stdin.readLine()) != null) {
            String trimmed = input.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.equalsIgnoreCase("quit")) break;

            String command = translate(playerName, trimmed);
            if (command != null) {
                out.println(command);
            } else {
                System.out.println("Unknown command. Use: color <COLOR> | totem <LETTER> | draw <ID> | end");
            }
            System.out.print("> ");
        }

        socket.close();
    }

    // ─────────────────────────────────────────────────────────
    // Formatting
    // ─────────────────────────────────────────────────────────

    private static String format(String line) {
        if (line.startsWith("STATE:")) {
            try {
                JsonElement el = JsonParser.parseString(line.substring(6));
                return "[STATE]\n" + formatJson(el, 0);
            } catch (Exception ignored) {}
        }
        if (line.startsWith("GAME_OVER:")) {
            String winners = line.substring(10);
            return "═══════════════════════════════\n" +
                   "  GAME OVER — Winner(s): " + winners + "\n" +
                   "═══════════════════════════════";
        }
        return "[SERVER] " + line;
    }

    /**
     * Formats a JsonElement with adaptive indentation:
     * if the compact representation fits within COMPACT_THRESHOLD characters
     * it stays on one line; otherwise it expands with indentation.
     */
    private static String formatJson(JsonElement el, int depth) {
        if (el.isJsonNull() || el.isJsonPrimitive()) {
            return el.toString();
        }
        String compact = compact(el);
        if (compact.length() <= COMPACT_THRESHOLD) {
            return compact;
        }
        String pad = INDENT.repeat(depth + 1);
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            StringBuilder sb = new StringBuilder("[\n");
            var list = arr.asList();
            for (int i = 0; i < list.size(); i++) {
                sb.append(pad).append(formatJson(list.get(i), depth + 1));
                if (i < list.size() - 1) sb.append(",");
                sb.append("\n");
            }
            return sb.append(INDENT.repeat(depth)).append("]").toString();
        }
        // JsonObject
        JsonObject obj = el.getAsJsonObject();
        StringBuilder sb = new StringBuilder("{\n");
        var entries = obj.entrySet().stream().toList();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            sb.append(pad)
              .append("\"").append(entry.getKey()).append("\": ")
              .append(formatJson(entry.getValue(), depth + 1));
            if (i < entries.size() - 1) sb.append(",");
            sb.append("\n");
        }
        return sb.append(INDENT.repeat(depth)).append("}").toString();
    }

    /** Renders a JsonElement as a single-line string (no whitespace added). */
    private static String compact(JsonElement el) {
        if (el.isJsonNull() || el.isJsonPrimitive()) return el.toString();
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            if (arr.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder("[");
            var list = arr.asList();
            for (int i = 0; i < list.size(); i++) {
                sb.append(compact(list.get(i)));
                if (i < list.size() - 1) sb.append(", ");
            }
            return sb.append("]").toString();
        }
        JsonObject obj = el.getAsJsonObject();
        if (obj.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        var entries = obj.entrySet().stream().toList();
        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            sb.append("\"").append(e.getKey()).append("\": ").append(compact(e.getValue()));
            if (i < entries.size() - 1) sb.append(", ");
        }
        return sb.append("}").toString();
    }

    // ─────────────────────────────────────────────────────────
    // Command translation
    // ─────────────────────────────────────────────────────────

    private static String translate(String playerName, String input) {
        String[] parts = input.split("\\s+", 2);
        String verb = parts[0].toLowerCase();
        String arg  = parts.length > 1 ? parts[1].trim() : "";

        return switch (verb) {
            case "color" -> "CHOOSE_COLOR:" + playerName + ":" + arg.toUpperCase();
            case "totem" -> "PLACE_TOTEM:"  + playerName + ":" + arg.toUpperCase();
            case "draw"  -> "DRAW_CARD:"    + playerName + ":" + arg;
            case "end"   -> "END_TURN:"     + playerName;
            default      -> null;
        };
    }
}
