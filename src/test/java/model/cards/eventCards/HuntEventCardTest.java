package model.cards.eventCards;

import model.cards.buildingCards.buildingEffects.onEventEffects.OnEventBuildingEffect;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("HuntEventCard Tests")
class HuntEventCardTest {

    @Test
    @DisplayName("resolve adds food and prestige based on 2 hunters and Era I")
    void resolveAddsFoodAndPrestigeFromHuntersERAI() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);
        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getHunterCount()).thenReturn(2);

        HuntEventCard card = new HuntEventCard(10, Era.ERA_I, 4, "test/front.png" , "test/front.png");

        card.resolve(List.of(player));

        verify(player, times(1)).addFood(2);
        verify(player, times(1)).addPrestigePoints(2); //2*1
    }

    @Test
    @DisplayName("resolve adds food and prestige based on 4 hunters and Era III")
    void resolveAddsFoodAndPrestigeFromHuntersERAIII() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);
        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getHunterCount()).thenReturn(4);

        HuntEventCard card = new HuntEventCard(10, Era.ERA_III, 4, "test/front.png" , "test/front.png");

        card.resolve(List.of(player));

        verify(player, times(1)).addFood(4);
        verify(player, times(1)).addPrestigePoints(12); //4*3
    }

    @Test
    @DisplayName("resolve handles zero hunters")
    void resolveHandlesZeroHunters() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);
        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getHunterCount()).thenReturn(0);

        HuntEventCard card = new HuntEventCard(11, Era.ERA_II, 4, "test/front.png" , "test/front.png");

        card.resolve(List.of(player));

        verify(player, times(1)).addFood(0);
        verify(player, times(1)).addPrestigePoints(0);
    }

    @Test
    @DisplayName("resolve triggers OnEventBuildingEffect.applyOnHunt for each player's building effects")
    void resolveTriggersOnEventBuildingEffects() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);
        OnEventBuildingEffect effect = mock(OnEventBuildingEffect.class);
        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getHunterCount()).thenReturn(2);
        when(tribe.getOnEventBuildingEffects()).thenReturn(List.of(effect));

        HuntEventCard card = new HuntEventCard(10, Era.ERA_I, 4, "test/front.png", "test/front.png");
        card.resolve(List.of(player));

        verify(effect, times(1)).applyOnHunt(player);
    }

    @Test
    @DisplayName("resolve handles multiple players with different hunter counts")
    void resolveMultiplePlayers() {
        Player p1 = mock(Player.class);
        Tribe t1 = mock(Tribe.class);

        Player p2 = mock(Player.class);
        Tribe t2 = mock(Tribe.class);  

        Player p3 = mock(Player.class);
        Tribe t3 = mock(Tribe.class);

        when(p1.getTribe()).thenReturn(t1);
        when(p2.getTribe()).thenReturn(t2);
        when(p3.getTribe()).thenReturn(t3);

        when(t1.getHunterCount()).thenReturn(3);
        when(t2.getHunterCount()).thenReturn(1);
        when(t3.getHunterCount()).thenReturn(0);


        HuntEventCard card = new HuntEventCard(12, Era.ERA_II, 4, "test/front.png" , "test/front.png");

        card.resolve(List.of(p1, p2, p3));

        verify(p1, times(1)).addFood(3);
        verify(p1, times(1)).addPrestigePoints(6); //3*2
        verify(p2, times(1)).addFood(1);
        verify(p2, times(1)).addPrestigePoints(2); //1*2
        verify(p3, times(1)).addFood(0);
        verify(p3, times(1)).addPrestigePoints(0);
    }
}
