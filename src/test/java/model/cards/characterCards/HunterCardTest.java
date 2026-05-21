package model.cards.characterCards;

import model.cards.characterCards.HunterCard;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HunterCardTest {

    @Mock private Player mockPlayer;
    @Mock private Tribe mockTribe;

    private static final int ID = 1;
    private static final Era ERA = Era.ERA_I;
    private static final int PLAYER_COUNT = 3;
    private static final String IMAGE_PATH = "front.png";
    private static final String BACK_IMAGE_PATH = "back.png";

    @Test
    @DisplayName("registerToTribe adds this builder card to player's tribe and not adds food")
    void testRegisterToTribe_WithoutTriggerIcon_RegistersOnly() {
        HunterCard cardWithoutIcon = new HunterCard(ID, ERA, PLAYER_COUNT, false, IMAGE_PATH, BACK_IMAGE_PATH);
        when(mockPlayer.getTribe()).thenReturn(mockTribe);

        cardWithoutIcon.registerToTribe(mockPlayer);

        // verifies card has been added to tribe
        verify(mockTribe, times(1)).addHunter(cardWithoutIcon);
        // verifies that no food has been added to the player
        verify(mockPlayer, never()).addFood(anyInt());
    }

    @Test
    @DisplayName("registerToTribe adds this builder card to player's tribe and adds food based on the number of hunters in the tribe")
    void testRegisterToTribe_WithTriggerIcon_RegistersAndAddsFood() {
        HunterCard cardWithIcon = new HunterCard(ID, ERA, PLAYER_COUNT, true, IMAGE_PATH, BACK_IMAGE_PATH);
        int expectedHuntersInTribe = 4; // considering the new hunter is already counted in the tribe

        when(mockPlayer.getTribe()).thenReturn(mockTribe);
        when(mockTribe.getHunterCount()).thenReturn(expectedHuntersInTribe);

        cardWithIcon.registerToTribe(mockPlayer);

        verify(mockTribe, times(1)).addHunter(cardWithIcon);
        verify(mockPlayer, times(1)).addFood(expectedHuntersInTribe);
    }
}