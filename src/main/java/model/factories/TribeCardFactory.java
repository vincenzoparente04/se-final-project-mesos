package model.factories;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.cards.TribeCard;
import model.cards.characterCards.*;
import model.cards.eventCards.*;
import model.enums.Era;
import model.enums.InventionIcon;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TribeCardFactory {

    private static int nextId = 1;

    /**
     * Container returned by createAll().
     * Keeps regular cards and final events separated without using shared static state.
     */
    public record TribeCardCollection(
            List<TribeCard> regularCards,
            List<TribeCard> finalEvents
    ) {}

    // Public API ────────────────────────────────────────────────────────────

    public static List<TribeCard> createRegularCards() {
        return createAll().regularCards();
    }

    public static List<TribeCard> createFinalEvents() {
        return createAll().finalEvents();
    }

    /**
     * Reads the full JSON and builds every TribeCard, returning them already
     * split into regular cards and final events.
     * isFinal is read here from JSON and used only to route each event card
     * into the correct list — it never leaks into the card model itself.
     */
    public static TribeCardCollection createAll() {
        nextId = 1;
        List<TribeCard> regularCards = new ArrayList<>();
        List<TribeCard> finalEvents  = new ArrayList<>();

        JsonObject root = loadJson("tribe_cards.json");

        regularCards.addAll(createHunters(root.getAsJsonArray("hunters")));
        regularCards.addAll(createShamans(root.getAsJsonArray("shamans")));
        regularCards.addAll(createBuilders(root.getAsJsonArray("builders")));
        regularCards.addAll(createInventors(root.getAsJsonArray("inventors")));
        regularCards.addAll(createArtists(root.getAsJsonArray("artists")));
        regularCards.addAll(createGatherers(root.getAsJsonArray("gatherers")));
        createEvents(root.getAsJsonArray("events"), regularCards, finalEvents);

        return new TribeCardCollection(regularCards, finalEvents);
    }

    // Internal ──────────────────────────────────────────────────────────────

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

    private static void createEvents(JsonArray array, List<TribeCard> regularCards, List<TribeCard> finalEvents) {
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

            if (isFinal) finalEvents.add(card);
            else         regularCards.add(card);
        }
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
