package model.factories;package model.factory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.buildingEffects.BuildingEffect;
import model.buildingEffects.BuildingFlagEffect;
import model.buildingEffects.EndGameEffects.EndGameBuildingEffect;
import model.buildingEffects.OnCharacterAcquiredEffects.BonusForCompletedSet;
import model.buildingEffects.OnCharacterAcquiredEffects.InventorsPairBonus;
import model.buildingEffects.OnCharacterAcquiredEffects.OnAcquireBuildingEffect;
import model.buildingEffects.OnEventEffects.ArtistBonusForCavePaintings;
import model.buildingEffects.OnEventEffects.HunterBonusForHuntEvent;
import model.buildingEffects.OnEventEffects.SustenanceDiscountEffect;
import model.buildingEffects.OnPickingEffects.OnPickingEffects;
import model.cards.buildingCards.BuildingCard;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BuildingCardFactory {

    private static int nextId = 200; // start from 200 to avoid collisions with tribe card IDs

    /**
     * Reads building_cards.json and creates all BuildingCards with their effects.
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
            String image = json.get("image").getAsString();
            String effectId = json.get("effectId").getAsString();

            BuildingEffect effect = createEffect(effectId);

            cards.add(new BuildingCard(id, era, minPlayers, foodCost, endGamePoints, effect, image));
        }

        return cards;
    }

    /**
     * Creates the correct BuildingEffect based on effectId from JSON.
     * This is the ONE place where effect construction is decided.
     * Each effectId maps to a specific effect class with the correct parameters.
     */
    private static BuildingEffect createEffect(String effectId) {
        return switch (effectId) {

            // ── Flag effects (set a boolean on the player) ──────────────────────
            case "shamanic_immunity"
                    -> new OnPickingEffects(p -> p.setShamanicImmunity(true));
            case "shamanic_double_points"
                    -> new OnPickingEffects(p -> p.setShamanicDoublePrestige(true));
            case "shamanic_extra_stars"
                    -> new OnPickingEffects(p -> p.setShamanicBonusIcons(true));
            case "extra_food_on_totem_return"
                    -> new OnPickingEffects(p -> p.setExtraFoodOnTotemReturn(true));
            case "extra_draw"
                    -> new OnPickingEffects(p -> p.setExtraDraw(true)) {
                @Override
                public void applyEffect(Player player) {

                }
            };

            // ── On Acquire effects ──────────────────────────────────────────────
            case "on_acquire_set"
                    -> new BonusForCompletedSet();
            case "on_acquire_pair"
                    -> new InventorsPairBonus();

            // ── On Event effects ────────────────────────────────────────────────
            case "on_sustenance_artist"
                    -> new SustenanceDiscountEffect(1, Tribe::getArtistCount);
            case "on_sustenance_gatherer"
                    -> new SustenanceDiscountEffect(1, Tribe::getGathererCount);
            case "on_sustenance_inventor"
                    -> new SustenanceDiscountEffect(1, Tribe::getInventorCount);
            case "on_hunt_bonus"
                    -> new HunterBonusForHuntEvent(1, 1);
            case "on_cave_paintings_bonus"
                    -> new ArtistBonusForCavePaintings(1);

            // ── End Game effects ────────────────────────────────────────────────
            case "end_game_count_hunters"
                    -> new EndGameBuildingEffect(3, Tribe::getHunterCount);
            case "end_game_count_shamans"
                    -> new EndGameBuildingEffect(3, Tribe::getShamanCount);
            case "end_game_count_artists"
                    -> new EndGameBuildingEffect(3, Tribe::getArtistCount);
            case "end_game_count_inventors"
                    -> new EndGameBuildingEffect(3, Tribe::getInventorCount);
            case "end_game_count_builders"
                    -> new EndGameBuildingEffect(3, Tribe::getBuilderCount);
            case "end_game_count_gatherers"
                    -> new EndGameBuildingEffect(3, Tribe::getGathererCount);
            case "end_game_count_sets"
                    -> new EndGameBuildingEffect(6, Tribe::countCompleteSets);
            case "end_game_double_builders"
                    -> new EndGameBuildingEffect(1, Tribe::calculateBuildersEndGamePoints);

            // ── No effect (super bonus card — 25PP are already in endGamePoints) ─
            case "none"
                    -> new BuildingFlagEffect(p -> {}); // no-op

            default -> throw new IllegalArgumentException("Unknown building effect: " + effectId);
        };
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
        InputStream is = BuildingCardFactory.class.getClassLoader().getResourceAsStream(filename);
        if (is == null) {
            throw new RuntimeException("Cannot find " + filename + " in resources");
        }
        return JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
    }
}


public class BuildingCardsFactory {
}
