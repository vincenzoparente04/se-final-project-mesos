package model.player;

import model.enums.TotemColor;
import model.enums.TotemLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Player Tests")
class PlayerTest {

    private Player player;

    // In PlayerTest.java
    @BeforeEach
    void setUp() {
        player = new Player("TestPlayer"); // Usa il costruttore esistente
        player.setColor(TotemColor.RED);   // Imposta il colore tramite setter
    }

    @DisplayName("removeFood - removes food when having enough")
    @Test
    void removeFoodWhenEnoughFood() {
        // Arrange
        player.addFood(10);
        int initialPrestigePoints = player.getPrestigePoints();

        // Act
        player.removeFood(5, 0);

        // Assert
        assertEquals(5, player.getFood(), "Should have 5 food remaining after removing 5 from 10");
        assertEquals(initialPrestigePoints, player.getPrestigePoints(), "Prestige points should not change");
    }

    @DisplayName("removeFood - removes all available food if not enough")
    @Test
    void removeFoodWhenNotEnoughFood() {
        // Arrange
        player.addFood(3);

        // Act
        player.removeFood(5, 1);

        // Assert
        assertEquals(0, player.getFood(), "Should have 0 food remaining after removing 5 from 3");
    }

    @DisplayName("removeFood - removes exactly the requested amount")
    @Test
    void removeFoodExactAmount() {
        // Arrange
        player.addFood(5);

        // Act
        player.removeFood(5, 0);

        // Assert
        assertEquals(0, player.getFood(), "Should have 0 food remaining after removing 5 from 5");
    }

    @DisplayName("removeFood - does not convert to prestige points with multiplier 0")
    @Test
    void removeFoodWithZeroMultiplier() {
        // Arrange
        player.addFood(2);
        player.addPrestigePoints(10);
        int initialPrestigePoints = player.getPrestigePoints();

        // Act
        player.removeFood(5, 0);

        // Assert
        assertEquals(0, player.getFood(), "Should have 0 food remaining");
        assertEquals(initialPrestigePoints, player.getPrestigePoints(),
                "Prestige points should not change with multiplier 0");
    }

    @DisplayName("removeFood - converts correctly with a high multiplier")
    @Test
    void removeFoodWithHighMultiplier() {
        // Arrange
        player.addFood(2);
        player.addPrestigePoints(20);
        int initialPrestigePoints = player.getPrestigePoints();

        // Act
        player.removeFood(7, 3);

        // Assert
        // 2 food removed, 5 remaining to be paid
        // 5 * 3 (multiplier) = 15 prestige points removed
        assertEquals(0, player.getFood(), "Should have 0 food remaining");
        assertEquals(initialPrestigePoints - 15, player.getPrestigePoints(),
                "Should remove 15 prestige points (5 food * 3 multiplier)");
    }

    @Test
    @DisplayName("Initial State - Verifies default values")
    void initialStateCorrectness() {
        assertAll("Verifies initial state of Player",
                () -> assertEquals(0, player.getFood(), "Food should be 0"),
                () -> assertEquals(0, player.getPrestigePoints(), "PP should be 0"),
                () -> assertNotNull(player.getTribe(), "Tribe should not be null"),
                () -> assertFalse(player.hasShamanicImmunity(), "Immunities should be false"),
                () -> assertFalse(player.hasShamanicDoublePrestige(), "Shamanic double prestige should be false"),
                () -> assertFalse(player.hasShamanicBonusIcons(), "Shamanic bonus icons should be false"),
                () -> assertFalse(player.hasExtraFoodOnTotemReturn(), "Extra food on totem return should be false"),
                () -> assertFalse(player.hasExtraDraw(), "Extra draw should be false")
        );
    }

    @Test
    @DisplayName("Prestige Points - Negative values cases")
    void prestigePointsCanBeNegative() {
        // Arrange
        player.addPrestigePoints(5);

        // Act
        player.removePrestigePoints(10);

        // Assert
        assertEquals(-5, player.getPrestigePoints(),
                "Rules let players have negative PP");
    }

    @Test
    @DisplayName("Boolean Flags - Verifies powers persistency")
    void testShamanicBonuses() {
        // Act
        player.setShamanicImmunity(true);
        player.setShamanicDoublePrestige(true);
        player.setShamanicBonusIcons(true);
        player.setExtraFoodOnTotemReturn(true);
        player.setExtraDraw(true);

        // Assert
        assertAll("Verify shamanic flags",
                () -> assertTrue(player.hasShamanicImmunity()),
                () -> assertTrue(player.hasShamanicDoublePrestige()),
                () -> assertTrue(player.hasShamanicBonusIcons()),
                () -> assertTrue(player.hasExtraFoodOnTotemReturn()),
                () -> assertTrue(player.hasExtraDraw())
        );
    }

    @Test
    @DisplayName("addFood - adds positive amount")
    void addFoodPositiveAmount() {
        // Act
        player.addFood(5);

        // Assert
        assertEquals(5, player.getFood(), "Should add 5 food");
    }

    @Test
    @DisplayName("addFood - adds zero amount")
    void addFoodZeroAmount() {
        // Act
        player.addFood(0);

        // Assert
        assertEquals(0, player.getFood(), "Should remain 0 food");
    }

    @Test
    @DisplayName("addPrestigePoints - adds positive amount")
    void addPrestigePointsPositiveAmount() {
        // Act
        player.addPrestigePoints(10);

        // Assert
        assertEquals(10, player.getPrestigePoints(), "Should add 10 prestige points");
    }

    @Test
    @DisplayName("addPrestigePoints - adds zero amount")
    void addPrestigePointsZeroAmount() {
        // Act
        player.addPrestigePoints(0);

        // Assert
        assertEquals(0, player.getPrestigePoints(), "Should remain 0 prestige points");
    }

    @Test
    @DisplayName("removePrestigePoints - removes positive amount")
    void removePrestigePointsPositiveAmount() {
        // Arrange
        player.addPrestigePoints(10);

        // Act
        player.removePrestigePoints(5);

        // Assert
        assertEquals(5, player.getPrestigePoints(), "Should remove 5 prestige points");
    }

    @Test
    @DisplayName("getName - returns correct name")
    void getNameReturnsCorrectName() {
        // Assert
        assertEquals("TestPlayer", player.getName(), "Should return the correct name");
    }

    @Test
    @DisplayName("setColor and getColor - sets and gets color")
    void setAndGetColor() {
        // Act
        player.setColor(TotemColor.BLUE);

        // Assert
        assertEquals(TotemColor.BLUE, player.getColor(), "Should set and get color correctly");
    }

    @Test
    @DisplayName("setLocation and getLocation - sets and gets location")
    void setAndGetLocation() {
        // Act
        player.setLocation(TotemLocation.TURN_ORDER_TILE);

        // Assert
        assertEquals(TotemLocation.TURN_ORDER_TILE, player.getLocation(), "Should set and get location correctly");
    }
}