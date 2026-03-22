package model.player;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Player Tests")
class PlayerTest {

    private Player player;

    @BeforeEach
    void setUp() {
        // Crea un nuovo player per ogni test
        player = new Player("TestPlayer", TotemColor.RED);
    }

    @DisplayName("removeFood - rimuove cibo quando ho abbastanza")
    @Test
    void removeFoodWhenEnoughFood() {
        // Arrange
        player.addFood(10);

        // Act
        player.removeFood(5, 0);

        // Assert
        assertEquals(5, player.getFood(), "Dovrebbe rimanere 5 cibo dopo aver rimosso 5 da 10");
    }

    @DisplayName("removeFood - rimuove tutto il cibo disponibile se non basta")
    @Test
    void removeFoodWhenNotEnoughFood() {
        // Arrange
        player.addFood(3);

        // Act
        player.removeFood(5, 1);

        // Assert
        assertEquals(0, player.getFood(), "Dovrebbe rimanere 0 cibo dopo aver rimosso 5 da 3");
    }

    @DisplayName("removeFood - converte il cibo mancante in prestige points")
    @Test
    void removeFoodConvertsToPrestigePoints() {
        // Arrange
        player.addFood(3);
        player.addPrestigePoints(10);
        int initialPrestigePoints = player.getPrestigePoints();

        // Act
        player.removeFood(5, 2);

        // Assert
        // 3 cibo rimosso, restano 2 da pagare con prestige points
        // 2 * 2 (moltiplicatore) = 4 prestige points rimossi
        assertEquals(0, player.getFood(), "Dovrebbe rimanere 0 cibo");
        assertEquals(initialPrestigePoints - 4, player.getPrestigePoints(),
            "Dovrebbe rimuovere 4 prestige points (2 cibo * 2 moltiplicatore)");
    }

    @DisplayName("removeFood - rimuove esattamente l'ammontare richiesto")
    @Test
    void removeFoodExactAmount() {
        // Arrange
        player.addFood(5);

        // Act
        player.removeFood(5, 0);

        // Assert
        assertEquals(0, player.getFood(), "Dovrebbe rimanere 0 cibo dopo aver rimosso 5 da 5");
    }

    @DisplayName("removeFood - con moltiplicatore 0 non converte a prestige points")
    @Test
    void removeFoodWithZeroMultiplier() {
        // Arrange
        player.addFood(2);
        player.addPrestigePoints(10);
        int initialPrestigePoints = player.getPrestigePoints();

        // Act
        player.removeFood(5, 0);

        // Assert
        assertEquals(0, player.getFood(), "Dovrebbe rimanere 0 cibo");
        assertEquals(initialPrestigePoints, player.getPrestigePoints(),
            "I prestige points non dovrebbero cambiare con moltiplicatore 0");
    }

    @DisplayName("removeFood - con moltiplicatore alto converte correttamente")
    @Test
    void removeFoodWithHighMultiplier() {
        // Arrange
        player.addFood(2);
        player.addPrestigePoints(20);
        int initialPrestigePoints = player.getPrestigePoints();

        // Act
        player.removeFood(7, 3);

        // Assert
        // 2 cibo rimosso, restano 5 da pagare
        // 5 * 3 (moltiplicatore) = 15 prestige points rimossi
        assertEquals(0, player.getFood(), "Dovrebbe rimanere 0 cibo");
        assertEquals(initialPrestigePoints - 15, player.getPrestigePoints(),
            "Dovrebbe rimuovere 15 prestige points (5 cibo * 3 moltiplicatore)");
    }
}