package model.cards.characterCards;

import model.cards.charachterCards.BuilderCard;
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
class BuilderCardTest {

    @Mock private Player player;
    @Mock private Tribe tribe;

    @Test
    @DisplayName("getters return builder discount and prestige points")
    void gettersReturnExpectedValues() {
        BuilderCard card = new BuilderCard(12, Era.ERA_II, 3, 2, 5, "front.png", "back.png");

        assertEquals(2, card.getBuilderDiscount());
        assertEquals(5, card.getPrestigePoints());
    }

    @Test
    @DisplayName("registerToTribe adds this builder card to player's tribe")
    void registerToTribeAddsBuilderToPlayersTribe() {
        BuilderCard card = new BuilderCard(7, Era.ERA_I, 2, 1, 3, "front.png", "back.png");

        when(player.getTribe()).thenReturn(tribe);
        card.registerToTribe(player);
        verify(tribe).addBuilder(card);
    }
}