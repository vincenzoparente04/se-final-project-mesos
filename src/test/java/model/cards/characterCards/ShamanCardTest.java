package model.cards.characterCards;

import model.cards.characterCards.ShamanCard;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShamanCardTest {

    @Mock private Player player;
    @Mock private Tribe tribe;

    @Test
    @DisplayName("getStarCount returns the correct number of stars")
    void getStarCountReturnsCorrectValue() {
        ShamanCard card = new ShamanCard(14, Era.ERA_II, 3, 4, "front.png", "back.png");

        assertEquals(4, card.getStarCount());
    }

    @Test
    @DisplayName("registerToTribe adds this shaman card to player's tribe")
    void registerToTribeAddsShamanToPlayersTribe() {
        ShamanCard card = new ShamanCard(9, Era.ERA_I, 2, 2, "front.png", "back.png");

        when(player.getTribe()).thenReturn(tribe);
        card.registerToTribe(player);
        verify(tribe).addShaman(card);
    }
}
