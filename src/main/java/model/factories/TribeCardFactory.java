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

    private static List<TribeCard> createHunters(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            Era era = parseEra(json);
            String backImage = "BackEra" + getEraNumber(era) + ".png";
            String image = json.get("image").getAsString() + ".png";

            cards.add(new HunterCard(
                    nextId++,
                    era,
                    json.get("minPlayers").getAsInt(),
                    json.get("triggerIcon").getAsBoolean(),
                    image,
                    backImage
            ));
        }
        return cards;
    }

    private static List<TribeCard> createShamans(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            Era era = parseEra(json);
            String backImage = "BackEra" + getEraNumber(era) + ".png";
            String image = json.get("image").getAsString() + ".png";

            cards.add(new ShamanCard(
                    nextId++,
                    era,
                    json.get("minPlayers").getAsInt(),
                    json.get("starCount").getAsInt(),
                    image,
                    backImage
            ));
        }
        return cards;
    }

    private static List<TribeCard> createBuilders(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            Era era = parseEra(json);
            String backImage = "BackEra" + getEraNumber(era) + ".png";
            String image = json.get("image").getAsString() + ".png";

            cards.add(new BuilderCard(
                    nextId++,
                    era,
                    json.get("minPlayers").getAsInt(),
                    json.get("discount").getAsInt(),
                    json.get("prestigePoints").getAsInt(),
                    image,
                    backImage
            ));
        }
        return cards;
    }

    private static List<TribeCard> createInventors(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            Era era = parseEra(json);
            String backImage = "BackEra" + getEraNumber(era) + ".png";
            String image = json.get("image").getAsString() + ".png";

            cards.add(new InventorCard(
                    nextId++,
                    era,
                    json.get("minPlayers").getAsInt(),
                    InventionIcon.valueOf(json.get("inventionIcon").getAsString()),
                    image,
                    backImage
            ));
        }
        return cards;
    }

    private static List<TribeCard> createArtists(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            Era era = parseEra(json);
            String backImage = "BackEra" + getEraNumber(era) + ".png";
            String image = json.get("image").getAsString() + ".png";

            cards.add(new ArtistCard(
                    nextId++,
                    era,
                    json.get("minPlayers").getAsInt(),
                    image,
                    backImage
            ));
        }
        return cards;
    }

    private static List<TribeCard> createGatherers(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            Era era = parseEra(json);
            String backImage = "BackEra" + getEraNumber(era) + ".png";
            String image = json.get("image").getAsString() + ".png";

            cards.add(new GathererCard(
                    nextId++,
                    era,
                    json.get("minPlayers").getAsInt(),
                    image,
                    backImage
            ));
        }
        return cards;
    }

    private static List<TribeCard> createEvents(JsonArray array) {
        List<TribeCard> cards = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();
            int id = nextId++;
            Era era = parseEra(json);
            int minPlayers = 2; // events are always for all player counts
            String image = json.get("image").getAsString() + ".png";
            boolean isFinal = json.get("isFinal").getAsBoolean();
            String eventType = json.get("eventType").getAsString();

            // Calcolo del retro: se è finale prende il dorso specifico, altrimenti quello dell'era
            String backImage = isFinal ? "BackFinalEvent.png" : "BackEra" + getEraNumber(era) + ".png";


            TribeCard card = switch (eventType) {
                case "Hunt"            -> new HuntEventCard(id, era, minPlayers, image, backImage);
                case "Sustenance"      -> new SustenanceEventCard(id, era, minPlayers, image, backImage);
                case "ShamanicRitual"  -> new ShamanicRitualEventCard(id, era, minPlayers, image, backImage);
                case "CavePaintings"   -> new CavePaintingsEventCard(id, era, minPlayers, image, backImage);
                default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
            };

            cards.add(card);
        }
        return cards;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private static Era parseEra(JsonObject json) {
        return switch (json.get("era").getAsString()) {
            case "I"   -> Era.ERA_I;
            case "II"  -> Era.ERA_II;
            case "III" -> Era.ERA_III;
            default -> throw new IllegalArgumentException("Unknown era: " + json.get("era"));
        };
    }

    private static int getEraNumber(Era era) {
        return switch (era) {
            case ERA_I -> 1;
            case ERA_II -> 2;
            case ERA_III -> 3;
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
