package model.eventCards;

import model.cards.eventCards.HuntEventCard;
import model.enums.CharacterType;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@DisplayName("HuntEventCard Tests")
class HuntEventCardTest {

    @Test
    @DisplayName("resolve adds food and prestige based on 2 hunters and eraI")
    void resolveAddsFoodAndPrestigeFromHuntersERAI() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);
        when(player.getTribe()).thenReturn(tribe);
        when(tribe.countByType(CharacterType.HUNTER)).thenReturn(2);

        HuntEventCard card = new HuntEventCard(10, Era.ERA_I, 4);

        card.resolve(List.of(player));

        verify(player).addFood(2);
        verify(player).addPrestigePoints(2); //2*1
    }

    @Test
    @DisplayName("resolve adds food and prestige based on 4 hunters and eraIII")
    void resolveAddsFoodAndPrestigeFromHuntersERAIII() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);
        when(player.getTribe()).thenReturn(tribe);
        when(tribe.countByType(CharacterType.HUNTER)).thenReturn(4);

        HuntEventCard card = new HuntEventCard(10, Era.ERA_III, 4);

        card.resolve(List.of(player));

        verify(player).addFood(4);
        verify(player).addPrestigePoints(12); //4*3
    }

    @Test
    @DisplayName("resolve handles zero hunters")
    void resolveHandlesZeroHunters() {
        Player player = mock(Player.class);
        Tribe tribe = mock(Tribe.class);
        when(player.getTribe()).thenReturn(tribe);
        when(tribe.countByType(CharacterType.HUNTER)).thenReturn(0);

        HuntEventCard card = new HuntEventCard(11, Era.ERA_II, 4);

        card.resolve(List.of(player));

        verify(player).addFood(0);
        verify(player).addPrestigePoints(0);
    }

    @Test
    @DisplayName("Multiple players with different hunter counts and eras (Similar to a real game scenario)")
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

        when(t1.countByType(CharacterType.HUNTER)).thenReturn(3);
        when(t2.countByType(CharacterType.HUNTER)).thenReturn(1); 
        when(t3.countByType(CharacterType.HUNTER)).thenReturn(0);


        HuntEventCard card = new HuntEventCard(12, Era.ERA_II, 4);

        card.resolve(List.of(p1, p2, p3));

        verify(p1, times(1)).addFood(3);
        verify(p1, times(1)).addPrestigePoints(6); //3*2
        verify(p2, times(1)).addFood(1);
        verify(p2, times(1)).addPrestigePoints(2); //1*2
        verify(p3, times(1)).addFood(0);
        verify(p3, times(1)).addPrestigePoints(0);
    }
}
