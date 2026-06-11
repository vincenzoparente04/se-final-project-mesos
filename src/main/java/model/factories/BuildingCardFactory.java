package model.factories;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.cards.buildingCards.buildingEffects.BuildingEffect;
import model.cards.buildingCards.buildingEffects.onPickingEffects.OnPickingEffects;
import model.cards.buildingCards.buildingEffects.endGameEffects.EndGameBuildingEffect;
import model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects.BonusForCompletedSet;
import model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects.InventorsPairBonus;
import model.cards.buildingCards.buildingEffects.onEventEffects.ArtistBonusForCavePaintings;
import model.cards.buildingCards.buildingEffects.onEventEffects.HunterBonusForHuntEvent;
import model.cards.buildingCards.buildingEffects.onEventEffects.SustenanceDiscountEffect;
import model.cards.buildingCards.BuildingCard;
import model.enums.Era;
import model.player.Tribe;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class responsible for parsing the building cards configuration from a JSON resource file
 * and instantiating {@link BuildingCard} objects. It handles the mapping of string-based effect
 * identifiers to complex, polymorphic {@link BuildingEffect} functional implementations.
 */
public class BuildingCardFactory {

    /**
     * Sequential identifier counter assigned to each newly instantiated building card.
     * Starts at an offset of 200 to strictly prevent collision domains with TribeCard IDs.
     */
    private static int nextId = 200; // start from 200 to avoid collisions with tribe card IDs

    /**
     * Reads the {@code building_cards.json} file, parses all structural properties, maps
     * associated game effects, and constructs the complete deck of building cards.
     * Resets the internal ID counter to 200 upon invocation.
     *
     * @return a mutable {@link List} containing all instantiated {@link BuildingCard} objects
     * @throws NullPointerException if any mandatory JSON key is missing
     * @throws IllegalArgumentException if an invalid era string or unknown effect identifier is encountered
     * @throws RuntimeException if the JSON file cannot be found or read from resources
     */
    public static List<BuildingCard> createAll() {
        nextId = 200;
        List<BuildingCard> cards = new ArrayList<>();

        JsonObject root = loadJson("building_cards.json");
        JsonArray array = root.getAsJsonArray("buildings");

        for (JsonElement el : array) {
            JsonObject json = el.getAsJsonObject();

            int id = nextId++;
            Era era = parseEra(json);
            int minPlayers = json.get("minPlayers").getAsInt();
            int foodCost = json.get("foodCost").getAsInt();
            int endGamePoints = json.get("endGamePoints").getAsInt();
            String image = json.get("image").getAsString() + ".png";
            String effectId = json.get("effectId").getAsString();

            // Calcolo del retro della carta
            String backImage = "BackBuildingEra" + getEraNumber(era) + ".png";

            BuildingEffect effect = createEffect(effectId);

            // Assicurati che il costruttore di BuildingCard accetti backImage come ultimo parametro
            cards.add(new BuildingCard(id, era, minPlayers, foodCost, endGamePoints, effect, effectId, image, backImage));
        }

        return cards;
    }

    /**
     * Resolves the textual effect identifier into its corresponding concrete {@link BuildingEffect}
     * implementation. This includes dynamic functional bindings (lambdas) for immediate player state
     * mutation, event-driven modifiers, and endgame scoring evaluations.
     *
     * @param effectId the string identifier mapped from the JSON configuration
     * @return the instantiated polymorphic {@link BuildingEffect}
     * @throws IllegalArgumentException if the provided {@code effectId} does not match any known structural branch
     */
    private static BuildingEffect createEffect(String effectId) {
        return switch (effectId) {
            case "shamanic_immunity" -> new OnPickingEffects(p -> p.setShamanicImmunity(true));
            case "shamanic_double_points" -> new OnPickingEffects(p -> p.setShamanicDoublePrestige(true));
            case "shamanic_extra_stars" -> new OnPickingEffects(p -> p.setShamanicBonusIcons(true));
            case "extra_food_on_totem_return" -> new OnPickingEffects(p -> p.setExtraFoodOnTotemReturn(true));
            case "extra_draw" -> new OnPickingEffects(p -> p.setExtraDraw(true)); // TODO CONTROLLA
            case "on_acquire_set" -> new BonusForCompletedSet();
            case "on_acquire_pair" -> new InventorsPairBonus();
            case "on_sustenance_artist" -> new SustenanceDiscountEffect(1, Tribe::getArtistCount);
            case "on_sustenance_gatherer" -> new SustenanceDiscountEffect(1, Tribe::getGathererCount);
            case "on_sustenance_inventor" -> new SustenanceDiscountEffect(1, Tribe::getInventorCount);
            case "on_hunt_bonus" -> new HunterBonusForHuntEvent(1, 1);
            case "on_cave_paintings_bonus" -> new ArtistBonusForCavePaintings(1);
            case "end_game_count_hunters" -> new EndGameBuildingEffect(3, Tribe::getHunterCount);
            case "end_game_count_shamans" -> new EndGameBuildingEffect(4, Tribe::getShamanCount);
            case "end_game_count_artists" -> new EndGameBuildingEffect(4, Tribe::getArtistCount);
            case "end_game_count_inventors" -> new EndGameBuildingEffect(2, Tribe::getInventorCount);
            case "end_game_count_builders" -> new EndGameBuildingEffect(4, Tribe::getBuilderCount);
            case "end_game_count_gatherers" -> new EndGameBuildingEffect(4, Tribe::getGathererCount);
            case "end_game_count_sets" -> new EndGameBuildingEffect(6, Tribe::countCompleteSets);
            case "end_game_double_builders" -> new EndGameBuildingEffect(1, Tribe::calculateBuildersEndGamePoints);
            case "none" -> new OnPickingEffects(p -> {});
            default -> throw new IllegalArgumentException("Unknown building effect: " + effectId);
        };
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
        InputStream is = BuildingCardFactory.class.getClassLoader().getResourceAsStream(filename);
        if (is == null) {
            throw new RuntimeException("Cannot find " + filename + " in resources");
        }
        return JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
    }
}
