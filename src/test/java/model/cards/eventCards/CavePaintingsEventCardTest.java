package model.cards.eventCards;

import model.cards.buildingCards.buildingEffects.onEventEffects.OnEventBuildingEffect;
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

@DisplayName("CavePaintingsEventCard Tests")
class CavePaintingsEventCardTest {

    @Test
    @DisplayName("resolve removes 2 prestige points when artists are below era threshold")
    void resolveRemovesPrestigeWhenArtistsAreBelowThreshold() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);
        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getArtistCount()).thenReturn(1);

        CavePaintingsEventCard card = new CavePaintingsEventCard(1, Era.ERA_III, 3, "test/front.png" , "test/front.png");

        card.resolve(List.of(player));

        verify(player, times(1)).removePrestigePoints(2);
        verify(player, never()).addPrestigePoints(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("resolve adds eraOrdinal * artists prestige points when threshold is met")
    void resolveAddsPrestigeWhenThresholdIsMet() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);
        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getArtistCount()).thenReturn(2);

        CavePaintingsEventCard card = new CavePaintingsEventCard(2, Era.ERA_II, 3, "test/front.png" , "test/front.png");

        card.resolve(List.of(player));

        verify(player, times(1)).addPrestigePoints(4); //2*2
        verify(player, never()).removePrestigePoints(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("resolve triggers OnEventBuildingEffect.applyOnCavePaintings for each player's building effects")
    void resolveTriggersOnEventBuildingEffects() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);
        OnEventBuildingEffect effect = mock(OnEventBuildingEffect.class);
        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getArtistCount()).thenReturn(3);
        when(tribe.getOnEventBuildingEffects()).thenReturn(List.of(effect));

        CavePaintingsEventCard card = new CavePaintingsEventCard(1, Era.ERA_II, 3, "test/front.png", "test/front.png");
        card.resolve(List.of(player));

        verify(effect, times(1)).applyOnCavePaintings(player);
    }

    @Test
    @DisplayName("resolve handles mixed artists counts: equal, above and below threshold")
    void resolveAddsPrestigeWhenArtistsAlmostEqualEraOrdinal() {
        //2 artists
        Player p1 = mock(Player.class);
        Tribe t1 = mock(Tribe.class);
        when(p1.getTribe()).thenReturn(t1);
        when(t1.getArtistCount()).thenReturn(2);

        //1 artist
        Player p2 = mock(Player.class);
        Tribe t2 = mock(Tribe.class);
        when(p2.getTribe()).thenReturn(t2);
        when(t2.getArtistCount()).thenReturn(1);

        //3 artists
        Player p3 = mock(Player.class);
        Tribe t3 = mock(Tribe.class);
        when(p3.getTribe()).thenReturn(t3);
        when(t3.getArtistCount()).thenReturn(3);

        CavePaintingsEventCard card = new CavePaintingsEventCard(3, Era.ERA_II, 3, "test/front.png" , "test/front.png");

        card.resolve(List.of(p1, p2, p3));

        verify(p1, times(1)).addPrestigePoints(4); //2*2
        verify(p1, never()).removePrestigePoints(org.mockito.ArgumentMatchers.anyInt());
        verify(p2, times(1)).removePrestigePoints(2);
        verify(p2, never()).addPrestigePoints(org.mockito.ArgumentMatchers.anyInt());
        verify(p3, times(1)).addPrestigePoints(6); //2*3
        verify(p3, never()).removePrestigePoints(org.mockito.ArgumentMatchers.anyInt());
    }
}
