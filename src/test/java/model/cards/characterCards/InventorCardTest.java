package model.cards.characterCards;

import model.cards.charachterCards.InventorCard;
import model.enums.Era;
import model.enums.InventionIcon;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class InventorCardTest {
    @Mock private Player player;
    @Mock private Tribe tribe;

    @BeforeEach
    public void setUp() {
        player = org.mockito.Mockito.mock(Player.class);
        tribe = org.mockito.Mockito.mock(Tribe.class);
    }

    @Test
    @DisplayName("getInventionIcon returns the correct icon")
    void getInventionIconReturnsCorrectValue() {
        InventorCard card = new InventorCard(10, Era.ERA_II, 3, InventionIcon.ICON_1, "front.png", "back.png");

        assertEquals(InventionIcon.ICON_1, card.getInventionIcon());
    }

    @Test
    @DisplayName("registerToTribe adds this inventor card to player's tribe")
    void registerToTribeAddsInventorToPlayersTribe() {
        InventorCard card = new InventorCard(8, Era.ERA_I, 2, InventionIcon.ICON_2, "front.png", "back.png");

        when(player.getTribe()).thenReturn(tribe);
        card.registerToTribe(player);
        verify(tribe).addInventor(card);
    }
}
