package model.factories;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.board.OfferTile;
import model.board.TurnOrderSlot;
import model.board.OfferTileAction.DrawCardsAction;
import model.board.OfferTileAction.OfferTileAction;
import model.board.OfferTileAction.TakeFoodAction;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class responsible for parsing the game board configuration from a JSON resource file
 * and instantiating the corresponding domain model components based on the player count.
 *
 */
public class BoardFactory {

    /**
     * An immutable container holding the isolated components required to initialize a game board.
     *
     * @param offerTiles the list of filtered {@link OfferTile} instances eligible for the game
     * @param turnOrderSlots the list of {@link TurnOrderSlot} instances configured for the specific player count
     * @param turnOrderTileImage the resource path string for the turn order background image
     */
    public record BoardComponents(
            List<OfferTile> offerTiles,
            List<TurnOrderSlot> turnOrderSlots,
            String turnOrderTileImage
    ) {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Reads the {@code board.json} configuration file and constructs all necessary board components
     * tailored to the specified number of participants.
     *
     * @param playerCount the number of players participating in the game session (expected range: 2 to 5)
     * @return a {@link BoardComponents} record encapsulating the instantiated tiles, slots, and assets
     * @throws NullPointerException if the configuration file contains missing structural fields
     * @throws IllegalArgumentException if no valid turn order configuration matches the given {@code playerCount}
     * @throws RuntimeException if the JSON file cannot be found or read from the application resources
     */
    public static BoardComponents createComponents(int playerCount) {
        JsonObject root = loadJson("board.json");

        List<OfferTile> offerTiles = buildOfferTiles(root.getAsJsonArray("offerTiles"), playerCount);

        TurnOrderData turnOrderData = buildTurnOrderData(root.getAsJsonArray("turnOrderSlots"), playerCount);

        return new BoardComponents(offerTiles, turnOrderData.slots(), turnOrderData.image());
    }

    // OfferTiles ────────────────────────────────────────────────────────────

    /**
     * Parses the JSON array containing offer tile definitions and filters them according to player count eligibility.
     *
     * @param array       the {@link JsonArray} containing the raw offer tile data
     * @param playerCount the current game session's player count used as a lower-bound filter threshold
     * @return a mutable {@link List} of instantiated {@link OfferTile} objects
     */
    private static List<OfferTile> buildOfferTiles(JsonArray array, int playerCount) {
        List<OfferTile> tiles = new ArrayList<>();

        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();

            // Step 1 — filter: discard tiles not eligible for this player count
            int minPlayers = json.get("minPlayers").getAsInt();
            if (minPlayers > playerCount) continue;

            // Step 2 — read basic fields
            char letter = json.get("letter").getAsString().charAt(0);
            String frontImage = json.get("frontImage").getAsString();
            String backImage  = json.get("backImage").getAsString();

            // Step 3 — build the correct action by reading the nested "action" object
            OfferTileAction action = buildAction(json.getAsJsonObject("action"));

            // Step 4 — build the tile
            tiles.add(new OfferTile(letter, action, frontImage, backImage));
        }

        return tiles;
    }

    /**
     * Parses a nested JSON action object and maps it to its specific polymorphic concrete implementation.
     *
     * @param action the {@link JsonObject} defining the action attributes and type identifier
     * @return the instantiated polymorphic {@link OfferTileAction} subtype
     * @throws IllegalArgumentException if the action type property does not match any known identifier
     */
    private static OfferTileAction buildAction(JsonObject action) {
        String type = action.get("type").getAsString();
        return switch (type) {
            case "food" -> new TakeFoodAction(action.get("amount").getAsInt());
            case "draw" -> new DrawCardsAction(action.get("topRow").getAsInt(), action.get("bottomRow").getAsInt());
            default -> throw new IllegalArgumentException("Unknown action type: " + type);
        };
    }

    // ── TurnOrderSlots ────────────────────────────────────────────────────────

    /**
     * An internal data transfer object to pair instantiated turn order slots with their associated visual asset path.
     *
     * @param slots the parsed {@link List} of {@link TurnOrderSlot} instances
     * @param image the resource path string for the group's front graphical asset
     */
    private record TurnOrderData(List<TurnOrderSlot> slots, String image) {}

    /**
     * Scans the turn order configuration array to find the exact structural match for the specified player count.
     *
     * @param array       the {@link JsonArray} containing configuration groups for various player counts
     * @param playerCount the exact number of players to match against the configuration groups
     * @return a {@link TurnOrderData} record holding the matching list of slots and asset metadata
     * @throws IllegalArgumentException if no layout group matches the specified {@code playerCount}
     */
    private static TurnOrderData buildTurnOrderData(JsonArray array, int playerCount) {
        for (JsonElement el : array) {
            JsonObject group = el.getAsJsonObject();

            // Exact match: each player count has its own distinct configuration
            if (group.get("minPlayers").getAsInt() != playerCount) continue;

            String image = group.get("frontImage").getAsString();
            List<TurnOrderSlot> slots = new ArrayList<>();

            for (JsonElement slotEl : group.getAsJsonArray("slots")) {
                JsonObject slotJson = slotEl.getAsJsonObject();
                int foodBonus  = slotJson.get("foodBonus").getAsInt();
                boolean isLast = slotJson.get("isLast").getAsBoolean();
                slots.add(new TurnOrderSlot(foodBonus, isLast));
            }

            return new TurnOrderData(slots, image);
        }

        throw new IllegalArgumentException(
                "No turnOrderSlots configuration found for playerCount=" + playerCount);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Loads and parses a JSON file from the application's classpath resources.
     *
     * @param filename the relative path or name of the target resource file
     * @return the parsed {@link JsonObject} root reference
     * @throws RuntimeException if the specified stream resource resolve evaluation returns {@code null}
     */
    private static JsonObject loadJson(String filename) {
        InputStream is = BoardFactory.class.getClassLoader().getResourceAsStream(filename);
        if (is == null) {
            throw new RuntimeException("Cannot find " + filename + " in resources");
        }
        return JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
    }
}