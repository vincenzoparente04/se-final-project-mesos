package model.cards.eventCards;

import model.cards.eventCards.ShamanicRitualEventCard;
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

@DisplayName("ShamanicRitualEventCard Tests")
class ShamanicRitualEventCardTest {

    @Test
    @DisplayName("Realistic case: resolve rewards max stars and penalizes min stars")
    void resolveRewardsMaxAndPenalizesMin() {
        Player lowPlayer = mock(Player.class);
        Player highPlayer = mock(Player.class);
        Player middlePlayer = mock(Player.class);

        Tribe lowTribe = mock(Tribe.class);
        Tribe highTribe = mock(Tribe.class);
        Tribe middleTribe = mock(Tribe.class);

        when(lowPlayer.getTribe()).thenReturn(lowTribe);
        when(highPlayer.getTribe()).thenReturn(highTribe);
        when(middlePlayer.getTribe()).thenReturn(middleTribe);

        when(lowTribe.getTotalShamanStars()).thenReturn(1);
        when(highTribe.getTotalShamanStars()).thenReturn(3);
        when(middleTribe.getTotalShamanStars()).thenReturn(2);

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(20, Era.ERA_III, 3);

        card.resolve(List.of(lowPlayer, highPlayer, middlePlayer));

        verify(highPlayer, times(1)).addPrestigePoints(15);
        verify(lowPlayer, times(1)).removePrestigePoints(7);

        verify(lowPlayer, never()).addPrestigePoints(15);
        verify(middlePlayer, never()).addPrestigePoints(15);
        verify(highPlayer, never()).removePrestigePoints(7);
        verify(middlePlayer, never()).removePrestigePoints(7);
    }

    @Test 
    @DisplayName("resolve handles ties correctly")
    void resolveHandlesTies() {
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

        when(t1.getTotalShamanStars()).thenReturn(5);
        when(t2.getTotalShamanStars()).thenReturn(5);
        when(t3.getTotalShamanStars()).thenReturn(2);
        when(t4.getTotalShamanStars()).thenReturn(2);

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(21, Era.ERA_II, 2);

        card.resolve(List.of(p1, p2, p3, p4));

        verify(p1, times(1)).addPrestigePoints(10);
        verify(p2, times(1)).addPrestigePoints(10);
        verify(p3, times(1)).removePrestigePoints(5);
        verify(p4, times(1)).removePrestigePoints(5);

        verify(p1, never()).removePrestigePoints(5);
        verify(p2, never()).removePrestigePoints(5);
        verify(p3, never()).addPrestigePoints(10);
        verify(p4, never()).addPrestigePoints(10);
    }

    @Test
    @DisplayName("resolve applies both effects when all players tie on shaman stars")
    void resolveAppliesBothEffectsOnTie() {
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);

        Tribe t1 = mock(Tribe.class);
        Tribe t2 = mock(Tribe.class);
        Tribe t3 = mock(Tribe.class);

        when(p1.getTribe()).thenReturn(t1);
        when(p2.getTribe()).thenReturn(t2);
        when(p3.getTribe()).thenReturn(t3);

        when(t1.getTotalShamanStars()).thenReturn(2);
        when(t2.getTotalShamanStars()).thenReturn(2);
        when(t3.getTotalShamanStars()).thenReturn(2);

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(21, Era.ERA_I, 2);

        card.resolve(List.of(p1, p2, p3));

        verify(p1, times(1)).addPrestigePoints(5);
        verify(p2, times(1)).addPrestigePoints(5);
        verify(p3, times(1)).addPrestigePoints(5);
        verify(p1, times(1)).removePrestigePoints(3);
        verify(p2, times(1)).removePrestigePoints(3);
        verify(p3, times(1)).removePrestigePoints(3);
    }

    @Test
    @DisplayName("resolve handles tie only on max stars")
    void resolveHandlesTieOnlyOnMaxStars() {
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

        when(t1.getTotalShamanStars()).thenReturn(5);
        when(t2.getTotalShamanStars()).thenReturn(5);
        when(t3.getTotalShamanStars()).thenReturn(3);
        when(t4.getTotalShamanStars()).thenReturn(1);

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(22, Era.ERA_II, 4);

        card.resolve(List.of(p1, p2, p3, p4));

        verify(p1, times(1)).addPrestigePoints(10);
        verify(p2, times(1)).addPrestigePoints(10);
        verify(p4, times(1)).removePrestigePoints(5);

        verify(p1, never()).removePrestigePoints(5);
        verify(p2, never()).removePrestigePoints(5);
        verify(p3, never()).removePrestigePoints(5);
        verify(p3, never()).addPrestigePoints(10);
        verify(p4, never()).addPrestigePoints(10);
    }

    @Test
    @DisplayName("resolve handles tie only on min stars")
    void resolveHandlesTieOnlyOnMinStars() {
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

        when(t1.getTotalShamanStars()).thenReturn(6);
        when(t2.getTotalShamanStars()).thenReturn(4);
        when(t3.getTotalShamanStars()).thenReturn(2);
        when(t4.getTotalShamanStars()).thenReturn(2);

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(23, Era.ERA_II, 4);

        card.resolve(List.of(p1, p2, p3, p4));

        verify(p1, times(1)).addPrestigePoints(10);
        verify(p3, times(1)).removePrestigePoints(5);
        verify(p4, times(1)).removePrestigePoints(5);

        verify(p1, never()).removePrestigePoints(5);
        verify(p2, never()).removePrestigePoints(5);
        verify(p2, never()).addPrestigePoints(10);
        verify(p3, never()).addPrestigePoints(10);
        verify(p4, never()).addPrestigePoints(10);
    }
}
