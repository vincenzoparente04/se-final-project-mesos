package model.cards.eventCards;

import model.cards.eventCards.ShamanicRitualEventCard;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
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

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(20, Era.ERA_III, 3, "test/front.png" , "test/front.png");

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

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(21, Era.ERA_II, 2, "test/front.png" , "test/front.png");

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
    @DisplayName("resolve applies reward to all and penalty to all when all players tie on shaman stars")
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

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(21, Era.ERA_I, 2, "test/front.png" , "test/front.png");

        card.resolve(List.of(p1, p2, p3));

        // All tie on max AND on min → everyone gets both reward and penalty
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

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(22, Era.ERA_II, 4, "test/front.png" , "test/front.png");

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

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(23, Era.ERA_II, 4, "test/front.png" , "test/front.png");

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

    @Test
    @DisplayName("resolve doubles prestige reward for winner with shamanicDoublePrestige building")
    void resolveShamanicDoublePrestigeDoublesWinnerReward() {
        Player winner = mock(Player.class);
        Player loser = mock(Player.class);
        Tribe winnerTribe = mock(Tribe.class);
        Tribe loserTribe = mock(Tribe.class);

        when(winner.getTribe()).thenReturn(winnerTribe);
        when(loser.getTribe()).thenReturn(loserTribe);
        when(winnerTribe.getTotalShamanStars()).thenReturn(5);
        when(loserTribe.getTotalShamanStars()).thenReturn(1);
        when(winner.hasShamanicDoublePrestige()).thenReturn(true);

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(30, Era.ERA_I, 2, "f.png", "b.png");
        card.resolve(List.of(winner, loser));

        verify(winner, times(1)).addPrestigePoints(10); // 5 * 2 doubled
        verify(loser, times(1)).removePrestigePoints(3);
        verify(winner, never()).removePrestigePoints(anyInt());
    }

    @Test
    @DisplayName("resolve skips penalty for loser with shamanicImmunity building")
    void resolveShamanicImmunityPreventsPenalty() {
        Player winner = mock(Player.class);
        Player loser = mock(Player.class);
        Tribe winnerTribe = mock(Tribe.class);
        Tribe loserTribe = mock(Tribe.class);

        when(winner.getTribe()).thenReturn(winnerTribe);
        when(loser.getTribe()).thenReturn(loserTribe);
        when(winnerTribe.getTotalShamanStars()).thenReturn(5);
        when(loserTribe.getTotalShamanStars()).thenReturn(1);
        when(loser.hasShamanicImmunity()).thenReturn(true);

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(31, Era.ERA_I, 2, "f.png", "b.png");
        card.resolve(List.of(winner, loser));

        verify(winner, times(1)).addPrestigePoints(5);
        verify(loser, never()).removePrestigePoints(anyInt());
    }

    @Test
    @DisplayName("resolve counts bonus icons from building when computing majority")
    void resolveBonusIconsTiltMajority() {
        Player playerWithBonus = mock(Player.class);
        Player otherPlayer = mock(Player.class);
        Tribe tribeWithBonus = mock(Tribe.class);
        Tribe otherTribe = mock(Tribe.class);

        when(playerWithBonus.getTribe()).thenReturn(tribeWithBonus);
        when(otherPlayer.getTribe()).thenReturn(otherTribe);
        when(tribeWithBonus.getTotalShamanStars()).thenReturn(2);
        when(otherTribe.getTotalShamanStars()).thenReturn(4);
        when(playerWithBonus.hasShamanicBonusIcons()).thenReturn(true); // 2 + 3 = 5 → wins

        ShamanicRitualEventCard card = new ShamanicRitualEventCard(32, Era.ERA_I, 2, "f.png", "b.png");
        card.resolve(List.of(playerWithBonus, otherPlayer));

        verify(playerWithBonus, times(1)).addPrestigePoints(5);
        verify(otherPlayer, times(1)).removePrestigePoints(3);
        verify(playerWithBonus, never()).removePrestigePoints(anyInt());
        verify(otherPlayer, never()).addPrestigePoints(5);
    }
}
