package model.cards.eventCards;

import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

        SustenanceEventCard card = new SustenanceEventCard(30, Era.ERA_III, 4, "test/front.png", "test/front.png");

        card.resolve(List.of(player));

        verify(player, times(1)).removeFoodWithPrestigePenalty(7, 3); // 10 - 3 = 7, era III multiplier = 3
    }

    @Test
    @DisplayName("resolve does nothing when gatherer discount exceeds characters")
    void resolveSkipsPlayerWhenDiscountCoversAllCharacters() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);

        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getTotalCharacterCount()).thenReturn(2);
        when(tribe.getTotalGatherersDiscount()).thenReturn(6);

        SustenanceEventCard card = new SustenanceEventCard(31, Era.ERA_II, 4, "test/front.png", "test/front.png");

        card.resolve(List.of(player));

        verify(player, never()).removeFoodWithPrestigePenalty(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("resolve handles multiple players with mixed food costs")
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

        SustenanceEventCard card = new SustenanceEventCard(32, Era.ERA_III, 4, "test/front.png", "test/front.png");

        card.resolve(List.of(p1, p2, p3, p4));

        verify(p1, times(1)).removeFoodWithPrestigePenalty(2, 3); // 5-3
        verify(p2, times(1)).removeFoodWithPrestigePenalty(2, 3); // 8-6
        verify(p3, never()).removeFoodWithPrestigePenalty(           // 3-6 negative, no food removed
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
        verify(p4, times(1)).removeFoodWithPrestigePenalty(6, 3); // 6-0
    }

    @Test
    @DisplayName("resolve does nothing for zero characters and zero gatherer discount")
    void resolvePlayerWithZeroCharactersAndZeroGathererDiscount() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);

        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getTotalCharacterCount()).thenReturn(0);
        when(tribe.getTotalGatherersDiscount()).thenReturn(0);

        SustenanceEventCard card = new SustenanceEventCard(33, Era.ERA_III, 4, "test/front.png", "test/front.png");

        card.resolve(List.of(player));

        verify(player, never()).removeFoodWithPrestigePenalty(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }
}
