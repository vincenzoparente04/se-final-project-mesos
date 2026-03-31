package model.cards;

import model.enums.Era;
import model.player.Player;
import model.rowsManager.CardVisitor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardTest {
    private static class DummyCard extends Card {
        public DummyCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
            super(id, era, playerCount, imagePath, backImagePath);
        }

        @Override
        public void registerToTribe(Player player) {}
        @Override
        public void accept(CardVisitor visitor) {}
    }

    @Test
    @DisplayName("Constructor initialize the state of the card")
    void testConstructorAndGetters() {
        int expectedId = 42;
        Era expectedEra = Era.ERA_II;
        int expectedPlayerCount = 4;
        String expectedImagePath = "front.png";
        String expectedBackImagePath = "back.png";

        Card dummyCard = new DummyCard(
                expectedId,
                expectedEra,
                expectedPlayerCount,
                expectedImagePath,
                expectedBackImagePath
        );

        assertEquals(expectedId, dummyCard.getId());
        assertEquals(expectedEra, dummyCard.getEra());
        assertEquals(expectedPlayerCount, dummyCard.getMinPlayerCount());
    }
}
