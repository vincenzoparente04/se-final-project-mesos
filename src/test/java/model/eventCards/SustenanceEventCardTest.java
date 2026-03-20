package model.eventCards;

import model.cards.eventCards.SustenanceEventCard;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.beans.Transient;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SustenanceEventCard Tests")
class SustenanceEventCardTest {

    @Test
    @DisplayName("resolve removes food based on characters minus gatherer discount")
    void resolveComputesFoodToPayAndCallsRemoveFood() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);

        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getTotalCharacterCount()).thenReturn(10);
        when(tribe.getTotalGatherersDiscount()).thenReturn(3);

        SustenanceEventCard card = new SustenanceEventCard(30, Era.ERA_III, 4);

        card.resolve(List.of(player));

        verify(player).removeFood(7 , 3); //10-3 
    }

    @Test
    @DisplayName("if gatherers discount exceeds characters, should not remove food and so should do nothing")
    void resolveForwardsNegativeFoodToPay() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);

        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getTotalCharacterCount()).thenReturn(2);
        when(tribe.getTotalGatherersDiscount()).thenReturn(6);

        SustenanceEventCard card = new SustenanceEventCard(31, Era.ERA_II, 4);

        card.resolve(List.of(player));

        verify(player, never()).removeFood(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
        //the removeFood method should not be called at all, since the event card has no effect on the player if the food to pay is negative
        //while i'm writing this comment the removeFood method can't handle negative food to pay, in fact it adds it up with the current food
    }

    @Test
    @DisplayName("Real game scenario: 3 players with different character counts and gatherer discounts")
    void resolveMultiplePlayers() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);
        Player p4 = mock(Player.class);

        Tribe t1 = mock(Tribe.class);
        Tribe t2 = mock(Tribe.class);
        Tribe t3 = mock(Tribe.class);
        Tribe t4 = mock(Tribe.class);

        when(p1.getTribe()).thenReturn(t1);
        when(p2.getTribe()).thenReturn(t2);
        when(p3.getTribe()).thenReturn(t3);
        when(p4.getTribe()).thenReturn(t4);

        when(t1.getTotalCharacterCount()).thenReturn(5);
        when(t1.getTotalGatherersDiscount()).thenReturn(3);

        when(t2.getTotalCharacterCount()).thenReturn(8);
        when(t2.getTotalGatherersDiscount()).thenReturn(6);

        when(t3.getTotalCharacterCount()).thenReturn(3);
        when(t3.getTotalGatherersDiscount()).thenReturn(6);

        when(t4.getTotalCharacterCount()).thenReturn(6);
        when(t4.getTotalGatherersDiscount()).thenReturn(0);

        SustenanceEventCard card = new SustenanceEventCard(32, Era.ERA_III, 4);

        card.resolve(List.of(p1, p2, p3, p4));

        verify(p1, times(1)).removeFood(2 , 3); //5-3
        verify(p2, times(1)).removeFood(2 , 3); //8-6
        verify(p3, never()).removeFood(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()); //3-6 negative, so no food removed
        verify(p4, times(1)).removeFood(6 , 3); //6-0
    }

    // EDGE CASES

    @Test 
    @DisplayName("Edge case: player with zero characters and zero gatherer discount")
    void resolvePlayerWithZeroCharactersAndZeroGathererDiscount() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);

        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getTotalCharacterCount()).thenReturn(0);
        when(tribe.getTotalGatherersDiscount()).thenReturn(0);

        SustenanceEventCard card = new SustenanceEventCard(33, Era.ERA_III, 4);

        card.resolve(List.of(player));

        verify(player, never()).removeFood(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }
}
