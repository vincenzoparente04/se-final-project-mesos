package model.factories;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.cards.TribeCard;
import model.cards.charachterCards.*;
import model.cards.eventCards.*;
import model.enums.Era;
import model.enums.InventionIcon;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TribeCardFactory {

    private static int nextId = 1;

    /**
     * Reads tribe_cards.json and creates all TribeCards (characters + events).
     * Each JSON section maps directly to a card subclass — no type field needed.
     */
    public static List<TribeCard> createAll() {
        nextId = 1;
        List<TribeCard> cards = new ArrayList<>();

        JsonObject root = loadJson("tribe_cards.json");

        cards.addAll(createHunters(root.getAsJsonArray("hunters")));
        cards.addAll(createShamans(root.getAsJsonArray("shamans")));
        cards.addAll(createBuilders(root.getAsJsonArray("builders")));
        cards.addAll(createInventors(root.getAsJsonArray("inventors")));
        cards.addAll(createArtists(root.getAsJsonArray("artists")));
        cards.addAll(createGatherers(root.getAsJsonArray("gatherers")));
        cards.addAll(createEvents(root.getAsJsonArray("events")));

        return cards;
    }

    // ── Hunters ──────────────────────────────────────────────────────────────────

    private static List<TribeCard> createHunters(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            cards.add(new HunterCard(
                    nextId++,
                    parseEra(json),
                    json.get("minPlayers").getAsInt(),
                    json.get("triggerIcon").getAsBoolean(),
                    json.get("image").getAsString()
            ));
        }
        return cards;
    }

    // ── Shamans ──────────────────────────────────────────────────────────────────

    private static List<TribeCard> createShamans(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            cards.add(new ShamanCard(
                    nextId++,
                    parseEra(json),
                    json.get("minPlayers").getAsInt(),
                    json.get("starCount").getAsInt(),
                    json.get("image").getAsString()
            ));
        }
        return cards;
    }

    // ── Builders ─────────────────────────────────────────────────────────────────

    private static List<TribeCard> createBuilders(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            cards.add(new BuilderCard(
                    nextId++,
                    parseEra(json),
                    json.get("minPlayers").getAsInt(),
                    json.get("discount").getAsInt(),
                    json.get("prestigePoints").getAsInt(),
                    json.get("image").getAsString()
            ));
        }
        return cards;
    }

    // ── Inventors ────────────────────────────────────────────────────────────────

    private static List<TribeCard> createInventors(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            cards.add(new InventorCard(
                    nextId++,
                    parseEra(json),
                    json.get("minPlayers").getAsInt(),
                    InventionIcon.valueOf(json.get("inventionIcon").getAsString()),
                    json.get("image").getAsString()
            ));
        }
        return cards;
    }

    // ── Artists ──────────────────────────────────────────────────────────────────

    private static List<TribeCard> createArtists(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            cards.add(new ArtistCard(
                    nextId++,
                    parseEra(json),
                    json.get("minPlayers").getAsInt(),
                    json.get("image").getAsString()
            ));
        }
        return cards;
    }

    // ── Gatherers ────────────────────────────────────────────────────────────────

    private static List<TribeCard> createGatherers(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            cards.add(new GathererCard(
                    nextId++,
                    parseEra(json),
                    json.get("minPlayers").getAsInt(),
                    json.get("image").getAsString()
            ));
        }
        return cards;
    }

    // ── Events ───────────────────────────────────────────────────────────────────

    /**
     * Events need a switch on eventType to create the correct subclass.
     * This is acceptable here — the factory is the ONE place where construction
     * decisions are made. The eventType is never used at runtime for branching.
     */
    private static List<TribeCard> createEvents(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            int id = nextId++;
            Era era = parseEra(json);
            int minPlayers = 2; // events are always for all player counts
            String image = json.get("image").getAsString();
            boolean isFinal = json.get("isFinal").getAsBoolean();
            String eventType = json.get("eventType").getAsString();

            TribeCard card = switch (eventType) {
                case "Hunt"            -> new HuntEventCard(id, era, minPlayers, isFinal, image);
                case "Sustenance"      -> new SustenanceEventCard(id, era, minPlayers, isFinal, image);
                case "ShamanicRitual"  -> new ShamanicRitualEventCard(id, era, minPlayers, isFinal, image);
                case "CavePaintings"   -> new CavePaintingsEventCard(id, era, minPlayers, isFinal, image);
                default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
            };

            cards.add(card);
        }
        return cards;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private static Era parseEra(JsonObject json) {
        return switch (json.get("era").getAsString()) {
            case "I"   -> Era.I;
            case "II"  -> Era.II;
            case "III" -> Era.III;
            default -> throw new IllegalArgumentException("Unknown era: " + json.get("era"));
        };
    }

    private static JsonObject loadJson(String filename) {
        InputStream is = TribeCardFactory.class.getClassLoader().getResourceAsStream(filename);
        if (is == null) {
            throw new RuntimeException("Cannot find " + filename + " in resources");
        }
        return JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
    }
}
