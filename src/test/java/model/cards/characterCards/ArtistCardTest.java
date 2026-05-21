package model.cards.characterCards;

import model.cards.characterCards.ArtistCard;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ArtistCardTest {

    @Mock private Player mockPlayer;
    @Mock private Tribe mockTribe;

    private static final int ID = 1;
    private static final Era ERA = Era.ERA_I;
    private static final int PLAYER_COUNT = 3;
    private static final String IMAGE_PATH = "front.png";
    private static final String BACK_IMAGE_PATH = "back.png";


    @Test
    @DisplayName("registerToTribe adds this artist card to player's tribe")
    void testRegisterToTribe() {
        ArtistCard artistCard = new ArtistCard(ID, ERA, PLAYER_COUNT,  IMAGE_PATH, BACK_IMAGE_PATH);
        when(mockPlayer.getTribe()).thenReturn(mockTribe);

        artistCard.registerToTribe(mockPlayer);

        // verifies card has been added to tribe
        verify(mockTribe, times(1)).addArtist(artistCard);
    }
}
