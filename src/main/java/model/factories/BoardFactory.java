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

public class BoardFactory {

    /**
     * Container returned by the factory.
     * Keeps OfferTiles and TurnOrderSlots separated so the caller (Board.setup) can pass each list to the correct object.
     */
    public record BoardComponents(
            List<OfferTile> offerTiles,
            List<TurnOrderSlot> turnOrderSlots,
            String turnOrderTileImage
    ) {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     *   Reads board.json and builds all board components for the given player count.
     * What this method does:
     *   1. Reads the "offerTiles" array and keeps only tiles whose minPlayers equal or less than playerCount.
     *      For each eligible tile it creates the correct OfferTileAction (TakeFoodAction or
     *      DrawCardsAction) by reading the nested "action" object, then builds the OfferTile.
     *   2. Reads the "turnOrderSlots" array and finds the group whose minPlayers == playerCount
     *      (exact match — each player count has its own distinct slot configuration).
     *      For each slot in that group it builds a TurnOrderSlot(foodBonus, isLast).
     *
     * @param playerCount number of players in the game (2–5)
     * @return a BoardComponents record containing the two ready-to-use lists
     */
    public static BoardComponents createComponents(int playerCount) {
        JsonObject root = loadJson("board.json");

        List<OfferTile> offerTiles = buildOfferTiles(root.getAsJsonArray("offerTiles"), playerCount);

        TurnOrderData turnOrderData = buildTurnOrderData(root.getAsJsonArray("turnOrderSlots"), playerCount);

        return new BoardComponents(offerTiles, turnOrderData.slots(), turnOrderData.image());
    }

    // OfferTiles ────────────────────────────────────────────────────────────

    private static List<OfferTile> buildOfferTiles(JsonArray array, int playerCount) {
        List<OfferTile> tiles = new ArrayList<>();

        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();

            // Step 1 — filter: discard tiles not eligible for this player count
            int minPlayers = json.get("minPlayers").getAsInt();
            if (minPlayers > playerCount) continue;

            // Step 2 — read basic fields
            char letter       = json.get("letter").getAsString().charAt(0);
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
     *   Reads the "action" object and returns the correct OfferTileAction.
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
     * Small private record to carry both the slots list and the image together
     * without exposing them as separate return values.
     */
    private record TurnOrderData(List<TurnOrderSlot> slots, String image) {}

    /**
     * Finds the turnOrderSlots group matching the exact player count,
     * then builds a TurnOrderSlot for each entry in that group.
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

    private static JsonObject loadJson(String filename) {
        InputStream is = BoardFactory.class.getClassLoader().getResourceAsStream(filename);
        if (is == null) {
            throw new RuntimeException("Cannot find " + filename + " in resources");
        }
        return JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
    }
}