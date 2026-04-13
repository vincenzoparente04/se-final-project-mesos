package model.cards.characterCards;

import model.cards.characterCards.GathererCard;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GathererCardTest {
    @Mock private Player player;
    @Mock private Tribe tribe;

    @Test
    @DisplayName("registerToTribe adds this gatherer card to player's tribe")
    void registerToTribeAddsGathererToPlayersTribe() {
        GathererCard card = new GathererCard(5, Era.ERA_I, 2, "front.png", "back.png");

        when(player.getTribe()).thenReturn(tribe);
        card.registerToTribe(player);
        verify(tribe).addGatherer(card);
    }
}