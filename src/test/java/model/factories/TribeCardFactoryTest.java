package model.factories;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.cards.Card;
import model.cards.TribeCard;
import model.cards.characterCards.ArtistCard;
import model.cards.characterCards.BuilderCard;
import model.cards.characterCards.GathererCard;
import model.cards.characterCards.HunterCard;
import model.cards.characterCards.InventorCard;
import model.cards.characterCards.ShamanCard;
import model.cards.eventCards.CavePaintingsEventCard;
import model.cards.eventCards.HuntEventCard;
import model.cards.eventCards.ShamanicRitualEventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.enums.Era;
import model.enums.InventionIcon;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TribeCardFactoryTest {

    @Test
    void createAllBuildsCardsFromJsonWithExpectedData() {
        JsonObject root = loadTribeJson();
        TribeCardFactory.TribeCardCollection collection = TribeCardFactory.createAll();

        Map<String, TribeCard> regularByImage = indexByImage(collection.regularCards());
        Map<String, TribeCard> finalByImage = indexByImage(collection.finalEvents());

        int expectedRegularCount = total(root, "hunters", "shamans", "builders", "inventors", "artists", "gatherers")
                + countNonFinalEvents(root.getAsJsonArray("events"));
        int expectedFinalCount = countFinalEvents(root.getAsJsonArray("events"));

        assertEquals(expectedRegularCount, collection.regularCards().size());
        assertEquals(expectedFinalCount, collection.finalEvents().size());

        int nextExpectedId = 1;
        nextExpectedId = assertCharacterSection(root.getAsJsonArray("hunters"), "hunters", regularByImage, nextExpectedId);
        nextExpectedId = assertCharacterSection(root.getAsJsonArray("shamans"), "shamans", regularByImage, nextExpectedId);
        nextExpectedId = assertCharacterSection(root.getAsJsonArray("builders"), "builders", regularByImage, nextExpectedId);
        nextExpectedId = assertCharacterSection(root.getAsJsonArray("inventors"), "inventors", regularByImage, nextExpectedId);
        nextExpectedId = assertCharacterSection(root.getAsJsonArray("artists"), "artists", regularByImage, nextExpectedId);
        nextExpectedId = assertCharacterSection(root.getAsJsonArray("gatherers"), "gatherers", regularByImage, nextExpectedId);
        assertEventSection(root.getAsJsonArray("events"), regularByImage, finalByImage, nextExpectedId);
    }

    @Test
    void createAllResetsIdsBetweenInvocations() {
        TribeCardFactory.TribeCardCollection firstCall = TribeCardFactory.createAll();
        TribeCardFactory.TribeCardCollection secondCall = TribeCardFactory.createAll();

        assertEquals(1, firstCall.regularCards().get(0).getId());
        assertEquals(1, secondCall.regularCards().get(0).getId());

        for (int i = 0; i < firstCall.regularCards().size(); i++) {
            assertEquals(firstCall.regularCards().get(i).getId(), secondCall.regularCards().get(i).getId());
        }
    }

    @Test
    void createEventsThrowsOnUnknownEventType() {
        JsonArray events = new JsonArray();
        JsonObject invalid = new JsonObject();
        invalid.addProperty("image", "FrontCardX");
        invalid.addProperty("era", "I");
        invalid.addProperty("eventType", "UnknownEvent");
        invalid.addProperty("isFinal", false);
        events.add(invalid);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> invokeCreateEvents(events));
        assertTrue(exception.getMessage().contains("Unknown event type"));
    }

    private static int assertCharacterSection(JsonArray section, String sectionName, Map<String, TribeCard> regularByImage, int nextExpectedId) {
        for (int i = 0; i < section.size(); i++) {
            JsonObject json = section.get(i).getAsJsonObject();
            String image = json.get("image").getAsString() + ".png";
            TribeCard card = regularByImage.get(image);

            assertNotNull(card, "Missing card for image: " + image);
            assertEquals(nextExpectedId++, card.getId());
            assertEquals(parseEra(json.get("era").getAsString()), card.getEra());
            assertEquals(json.get("minPlayers").getAsInt(), card.getMinPlayerCount());
            assertEquals("BackEra" + eraNumber(card.getEra()) + ".png", getCardField(card, "backImagePath"));
            assertEquals(image, getCardField(card, "imagePath"));

            switch (sectionName) {
                case "hunters" -> {
                    assertInstanceOf(HunterCard.class, card);
                    assertEquals(json.get("triggerIcon").getAsBoolean(), getBooleanField(card, HunterCard.class, "triggerIcon"));
                }
                case "shamans" -> {
                    assertInstanceOf(ShamanCard.class, card);
                    assertEquals(json.get("starCount").getAsInt(), ((ShamanCard) card).getStarCount());
                }
                case "builders" -> {
                    assertInstanceOf(BuilderCard.class, card);
                    BuilderCard builder = (BuilderCard) card;
                    assertEquals(json.get("discount").getAsInt(), builder.getBuilderDiscount());
                    assertEquals(json.get("prestigePoints").getAsInt(), builder.getPrestigePoints());
                }
                case "inventors" -> {
                    assertInstanceOf(InventorCard.class, card);
                    assertEquals(InventionIcon.valueOf(json.get("inventionIcon").getAsString()), ((InventorCard) card).getInventionIcon());
                }
                case "artists" -> assertInstanceOf(ArtistCard.class, card);
                case "gatherers" -> assertInstanceOf(GathererCard.class, card);
                default -> fail("Unhandled section: " + sectionName);
            }
        }
        return nextExpectedId;
    }

    private static void assertEventSection(JsonArray events, Map<String, TribeCard> regularByImage, Map<String, TribeCard> finalByImage, int nextExpectedId) {
        for (int i = 0; i < events.size(); i++) {
            JsonObject json = events.get(i).getAsJsonObject();
            String image = json.get("image").getAsString() + ".png";
            boolean isFinal = json.get("isFinal").getAsBoolean();

            TribeCard card = isFinal ? finalByImage.get(image) : regularByImage.get(image);
            assertNotNull(card, "Missing event card for image: " + image);

            assertEquals(nextExpectedId++, card.getId());
            assertEquals(parseEra(json.get("era").getAsString()), card.getEra());
            assertEquals(2, card.getMinPlayerCount());
            assertEquals(image, getCardField(card, "imagePath"));
            assertEquals(isFinal ? "BackFinalEvent.png" : "BackEra" + eraNumber(card.getEra()) + ".png", getCardField(card, "backImagePath"));

            String eventType = json.get("eventType").getAsString();
            switch (eventType) {
                case "Hunt" -> assertInstanceOf(HuntEventCard.class, card);
                case "Sustenance" -> assertInstanceOf(SustenanceEventCard.class, card);
                case "ShamanicRitual" -> assertInstanceOf(ShamanicRitualEventCard.class, card);
                case "CavePaintings" -> assertInstanceOf(CavePaintingsEventCard.class, card);
                default -> fail("Unhandled event type in test: " + eventType);
            }
        }
    }

    private static Map<String, TribeCard> indexByImage(List<TribeCard> cards) {
        Map<String, TribeCard> byImage = new HashMap<>();
        for (TribeCard card : cards) {
            byImage.put(getCardField(card, "imagePath"), card);
        }
        return byImage;
    }

    private static JsonObject loadTribeJson() {
        InputStream is = TribeCardFactory.class.getClassLoader().getResourceAsStream("tribe_cards.json");
        assertNotNull(is, "tribe_cards.json should be available in test resources");
        return JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
    }

    private static int total(JsonObject root, String... sections) {
        int total = 0;
        for (String section : sections) {
            total += root.getAsJsonArray(section).size();
        }
        return total;
    }

    private static int countFinalEvents(JsonArray events) {
        int count = 0;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getAsJsonObject().get("isFinal").getAsBoolean()) {
                count++;
            }
        }
        return count;
    }

    private static int countNonFinalEvents(JsonArray events) {
        return events.size() - countFinalEvents(events);
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

    private static String getCardField(TribeCard card, String fieldName) {
        try {
            Field field = Card.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(card);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to read field '" + fieldName + "'", e);
        }
    }

    private static boolean getBooleanField(Object target, Class<?> owner, String fieldName) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getBoolean(target);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to read boolean field '" + fieldName + "'", e);
        }
    }

    private static void invokeCreateEvents(JsonArray events) {
        try {
            Method method = TribeCardFactory.class.getDeclaredMethod("createEvents", JsonArray.class, List.class, List.class);
            method.setAccessible(true);
            method.invoke(null, events, new ArrayList<>(), new ArrayList<>());
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke createEvents", e);
        }
    }
}

