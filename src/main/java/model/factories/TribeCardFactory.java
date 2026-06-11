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

/**
 * Factory class responsible for parsing the tribe cards configuration from a JSON resource file
 * and instantiating polymorphic concrete implementations of {@link TribeCard}.
 * Manages an internal sequential identifier counter for card instantiation.
 */
public class TribeCardFactory {
    /**
     * Sequential identifier counter assigned to each newly instantiated card.
     */
    private static int nextId = 1;

    /**
     * An immutable container holding the complete collection of instantiated tribe cards,
     * segregated into regular game cards and final event cards.
     *
     * @param regularCards the list of standard character and non-final event cards
     * @param finalEvents  the list of final event cards used for end-game scoring
     */
    public record TribeCardCollection(
            List<TribeCard> regularCards,
            List<TribeCard> finalEvents
    ) {}

    // Public API ────────────────────────────────────────────────────────────

    /**
     * Parses the configuration file and returns only the standard cards.
     *
     * @return a mutable {@link List} of regular {@link TribeCard} instances
     * @throws NullPointerException if the configuration file contains missing structural fields
     * @throws RuntimeException if the JSON file cannot be found or read from resources
     */
    public static List<TribeCard> createRegularCards() {
        return createAll().regularCards();
    }

    /**
     * Parses the configuration file and returns only the final event cards.
     *
     * @return a mutable {@link List} of final event {@link TribeCard} instances
     * @throws NullPointerException if the configuration file contains missing structural fields
     * @throws RuntimeException if the JSON file cannot be found or read from resources
     */
    public static List<TribeCard> createFinalEvents() {
        return createAll().finalEvents();
    }

    /**
     * Reads the {@code tribe_cards.json} file, parses all card subtypes, and segregates them
     * into regular and final categories. Resets the internal ID counter to 1 upon invocation.
     *
     * @return a {@link TribeCardCollection} containing the complete mapped card infrastructure
     * @throws NullPointerException if any mandatory JSON key is missing
     * @throws IllegalArgumentException if an invalid era string or polymorphic event type is encountered
     * @throws RuntimeException if the resource stream cannot be opened
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

    /**
     * Parses the JSON array representing hunters and instantiates {@link HunterCard} objects.
     *
     * @param array the {@link JsonArray} containing hunter card raw data
     * @return a mutable {@link List} of instantiated hunter cards
     */
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

    /**
     * Parses the JSON array representing shamans and instantiates {@link ShamanCard} objects.
     *
     * @param array the {@link JsonArray} containing shaman card raw data
     * @return a mutable {@link List} of instantiated shaman cards
     */
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

    /**
     * Parses the JSON array representing builders and instantiates {@link BuilderCard} objects.
     *
     * @param array the {@link JsonArray} containing builder card raw data
     * @return a mutable {@link List} of instantiated builder cards
     */
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

    /**
     * Parses the JSON array representing inventors and instantiates {@link InventorCard} objects.
     *
     * @param array the {@link JsonArray} containing inventor card raw data
     * @return a mutable {@link List} of instantiated inventor cards
     * @throws IllegalArgumentException if the {@code inventionIcon} text value does not match any enum constant
     */
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

    /**
     * Parses the JSON array representing artists and instantiates {@link ArtistCard} objects.
     *
     * @param array the {@link JsonArray} containing artist card raw data
     * @return a mutable {@link List} of instantiated artist cards
     */
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

    /**
     * Parses the JSON array representing gatherers and instantiates {@link GathererCard} objects.
     *
     * @param array the {@link JsonArray} containing gatherer card raw data
     * @return a mutable {@link List} of instantiated gatherer cards
     */
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

    /**
     * Parses the JSON array representing event cards polimorphically and populates
     * the target lists depending on the finality status flag.
     *
     * @param array the {@link JsonArray} containing event card raw data
     * @param regularCards the destination list for standard event cards
     * @param finalEvents  the destination list for game-ending event cards
     * @throws IllegalArgumentException if the mapped event type field does not match any known structural branch
     */
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
                case "Hunt" -> new HuntEventCard(id, era, minPlayers, image, backImage);
                case "Sustenance" -> new SustenanceEventCard(id, era, minPlayers, image, backImage);
                case "ShamanicRitual" -> new ShamanicRitualEventCard(id, era, minPlayers, image, backImage);
                case "CavePaintings" -> new CavePaintingsEventCard(id, era, minPlayers, image, backImage);
                default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
            };

            if (isFinal) finalEvents.add(card);
            else regularCards.add(card);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Maps the textual JSON representation of an era to its strong-typed enum constant counterpart.
     *
     * @param json the context {@link JsonObject} containing the era property
     * @return the associated {@link Era} enum instance
     * @throws IllegalArgumentException if the text identifier does not correspond to a valid game era notation
     */
    private static Era parseEra(JsonObject json) {
        return switch (json.get("era").getAsString()) {
            case "I"   -> Era.ERA_I;
            case "II"  -> Era.ERA_II;
            case "III" -> Era.ERA_III;
            default -> throw new IllegalArgumentException("Unknown era: " + json.get("era"));
        };
    }

    /**
     * Resolves the primitive integer numeric digit mapping representing a chronological enum game era.
     *
     * @param era the {@link Era} constant reference to resolve
     * @return an integer constant matching the sequence (1, 2, or 3)
     */
    private static int getEraNumber(Era era) {
        return switch (era) {
            case ERA_I -> 1;
            case ERA_II -> 2;
            case ERA_III -> 3;
        };
    }

    /**
     * Encapsulates the classpath resource file stream acquisition logic and parses its contents.
     *
     * @param filename the relative path or name of the target resource file
     * @return the parsed {@link JsonObject} root reference
     * @throws RuntimeException if the specified stream resource resolve evaluation returns {@code null}
     */
    private static JsonObject loadJson(String filename) {
        InputStream is = TribeCardFactory.class.getClassLoader().getResourceAsStream(filename);
        if (is == null) {
            throw new RuntimeException("Cannot find " + filename + " in resources");
        }
        return JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
    }
}
