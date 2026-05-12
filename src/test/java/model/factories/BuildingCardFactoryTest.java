package model.factories;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import model.buildingEffects.BuildingEffect;
import model.buildingEffects.EndGameEffects.EndGameBuildingEffect;
import model.buildingEffects.OnCharacterAcquiredEffects.BonusForCompletedSet;
import model.buildingEffects.OnCharacterAcquiredEffects.InventorsPairBonus;
import model.buildingEffects.OnEventEffects.ArtistBonusForCavePaintings;
import model.buildingEffects.OnEventEffects.HunterBonusForHuntEvent;
import model.buildingEffects.OnEventEffects.SustenanceDiscountEffect;
import model.buildingEffects.OnPickingEffects.OnPickingEffects;
import model.cards.Card;
import model.cards.buildingCards.BuildingCard;
import model.enums.Era;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BuildingCardFactoryTest {

    @Test
    void createAllBuildsCardsFromJsonWithExpectedData() {
        List<BuildingCard> cards = BuildingCardFactory.createAll();
        JsonArray expectedCards = loadBuildingsJson();

        assertEquals(expectedCards.size(), cards.size());

        for (int i = 0; i < expectedCards.size(); i++) {
            JsonObject expected = expectedCards.get(i).getAsJsonObject();
            BuildingCard card = cards.get(i);

            assertEquals(200 + i, card.getId());
            assertEquals(parseEra(expected.get("era").getAsString()), card.getEra());
            assertEquals(expected.get("minPlayers").getAsInt(), card.getMinPlayerCount());
            assertEquals(expected.get("foodCost").getAsInt(), card.getFoodCost());
            assertEquals(expected.get("endGamePoints").getAsInt(), card.getEndGamePoints());

            assertEquals(expected.get("image").getAsString() + ".png", getCardField(card, "imagePath"));
            assertEquals("BackBuildingEra" + eraNumber(card.getEra()) + ".png", getCardField(card, "backImagePath"));

            String effectId = expected.get("effectId").getAsString();
            assertInstanceOf(expectedEffectClasses().get(effectId), card.getEffect());
        }
    }

    @Test
    void createEffectThrowsOnUnknownEffectId() {
        assertThrows(IllegalArgumentException.class, () -> invokeCreateEffect("unknown_effect"));
    }

    @Test
    void shamanicImmunityLambdaSetsFlag() {
        Player player = mock(Player.class);
        ((OnPickingEffects) invokeCreateEffect("shamanic_immunity")).registerSelf(player);
        verify(player).setShamanicImmunity(true);
    }

    @Test
    void shamanicDoublePointsLambdaSetsFlag() {
        Player player = mock(Player.class);
        ((OnPickingEffects) invokeCreateEffect("shamanic_double_points")).registerSelf(player);
        verify(player).setShamanicDoublePrestige(true);
    }

    @Test
    void shamanicExtraStarsLambdaSetsFlag() {
        Player player = mock(Player.class);
        ((OnPickingEffects) invokeCreateEffect("shamanic_extra_stars")).registerSelf(player);
        verify(player).setShamanicBonusIcons(true);
    }

    @Test
    void extraFoodOnTotemReturnLambdaSetsFlag() {
        Player player = mock(Player.class);
        ((OnPickingEffects) invokeCreateEffect("extra_food_on_totem_return")).registerSelf(player);
        verify(player).setExtraFoodOnTotemReturn(true);
    }

    @Test
    void extraDrawLambdaSetsFlag() {
        Player player = mock(Player.class);
        ((OnPickingEffects) invokeCreateEffect("extra_draw")).registerSelf(player);
        verify(player).setExtraDraw(true);
    }

    @Test
    void noneLambdaIsNoOp() {
        Player player = mock(Player.class);
        ((OnPickingEffects) invokeCreateEffect("none")).registerSelf(player);
        // no-op — just verifying it doesn't throw
    }

    @Test
    void parseEraThrowsOnUnknownEraString() {
        JsonObject fakeJson = new JsonObject();
        fakeJson.add("era", new JsonPrimitive("INVALID"));

        assertThrows(IllegalArgumentException.class, () -> invokeParseEra(fakeJson));
    }

    @Test
    void loadJsonThrowsWhenResourceNotFound() {
        assertThrows(RuntimeException.class, () -> invokeLoadJson("nonexistent_file.json"));
    }

    private static JsonArray loadBuildingsJson() {
        InputStream is = BuildingCardFactory.class.getClassLoader().getResourceAsStream("building_cards.json");
        assertNotNull(is, "building_cards.json should be available in test resources");

        JsonObject root = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
        return root.getAsJsonArray("buildings");
    }

    private static Era parseEra(String era) {
        return switch (era) {
            case "I" -> Era.ERA_I;
            case "II" -> Era.ERA_II;
            case "III" -> Era.ERA_III;
            default -> throw new IllegalArgumentException("Unknown test era: " + era);
        };
    }

    private static int eraNumber(Era era) {
        return switch (era) {
            case ERA_I -> 1;
            case ERA_II -> 2;
            case ERA_III -> 3;
        };
    }

    private static String getCardField(BuildingCard card, String fieldName) {
        try {
            Field field = Card.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(card);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to read field '" + fieldName + "'", e);
        }
    }

    private static BuildingEffect invokeCreateEffect(String effectId) {
        try {
            Method method = BuildingCardFactory.class.getDeclaredMethod("createEffect", String.class);
            method.setAccessible(true);
            return (BuildingEffect) method.invoke(null, effectId);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke createEffect", e);
        }
    }

    private static Era invokeParseEra(JsonObject json) {
        try {
            Method method = BuildingCardFactory.class.getDeclaredMethod("parseEra", JsonObject.class);
            method.setAccessible(true);
            return (Era) method.invoke(null, json);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke parseEra", e);
        }
    }

    private static JsonObject invokeLoadJson(String filename) {
        try {
            Method method = BuildingCardFactory.class.getDeclaredMethod("loadJson", String.class);
            method.setAccessible(true);
            return (JsonObject) method.invoke(null, filename);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke loadJson", e);
        }
    }

    private static Map<String, Class<? extends BuildingEffect>> expectedEffectClasses() {
        return Map.ofEntries(
                Map.entry("shamanic_immunity", OnPickingEffects.class),
                Map.entry("shamanic_double_points", OnPickingEffects.class),
                Map.entry("shamanic_extra_stars", OnPickingEffects.class),
                Map.entry("extra_food_on_totem_return", OnPickingEffects.class),
                Map.entry("extra_draw", OnPickingEffects.class),
                Map.entry("on_acquire_set", BonusForCompletedSet.class),
                Map.entry("on_acquire_pair", InventorsPairBonus.class),
                Map.entry("on_sustenance_artist", SustenanceDiscountEffect.class),
                Map.entry("on_sustenance_gatherer", SustenanceDiscountEffect.class),
                Map.entry("on_sustenance_inventor", SustenanceDiscountEffect.class),
                Map.entry("on_hunt_bonus", HunterBonusForHuntEvent.class),
                Map.entry("on_cave_paintings_bonus", ArtistBonusForCavePaintings.class),
                Map.entry("end_game_count_hunters", EndGameBuildingEffect.class),
                Map.entry("end_game_count_shamans", EndGameBuildingEffect.class),
                Map.entry("end_game_count_artists", EndGameBuildingEffect.class),
                Map.entry("end_game_count_inventors", EndGameBuildingEffect.class),
                Map.entry("end_game_count_builders", EndGameBuildingEffect.class),
                Map.entry("end_game_count_gatherers", EndGameBuildingEffect.class),
                Map.entry("end_game_count_sets", EndGameBuildingEffect.class),
                Map.entry("end_game_double_builders", EndGameBuildingEffect.class),
                Map.entry("none", OnPickingEffects.class)
        );
    }
}
