package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameModel Tests")
class GameModelTest {

    private GameModel gameModel;

    @BeforeEach
    void setUp() {
        // Inizializza il GameModel prima di ogni test
        gameModel = new GameModel();
    }

    @Test
    @DisplayName("GameModel should be created successfully")
    void testGameModelCreation() {
        assertNotNull(gameModel, "GameModel non dovrebbe essere null");
    }

    // Aggiungi i tuoi test qui
    // Esempio:
    // @Test
    // void testGameInitialization() {
    //     assertTrue(gameModel.isInitialized());
    // }
}

