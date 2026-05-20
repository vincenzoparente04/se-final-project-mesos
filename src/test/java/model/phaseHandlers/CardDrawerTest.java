package model.phaseHandlers;

import model.board.OfferTileAction.OfferTileAction;
import model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects.OnAcquireBuildingEffect;
import model.cards.buildingCards.BuildingCard;
import model.cards.characterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.player.Player;
import model.player.Tribe;
import model.rowsManager.RowsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@DisplayName("CardDrawer Tests")
class CardDrawerTest {

    private Player player;
    private Tribe tribe;
    private RowsManager rowsManager;
    private OfferTileAction action;
    private CardDrawer cardDrawer;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        tribe = mock(Tribe.class);
        rowsManager = mock(RowsManager.class);
        action = mock(OfferTileAction.class);

        when(player.getTribe()).thenReturn(tribe);
        when(tribe.getOnAcquireBuildingEffects()).thenReturn(List.of());

        cardDrawer = new CardDrawer(player, rowsManager, action);
    }

    @Test
    @DisplayName("visit(CharacterCard) registers card to tribe, calls performDraw and removes card")
    void visitCharacterCardRegistersToTribeCallsPerformDrawAndRemoves() {
        CharacterCard card = mock(CharacterCard.class);
        when(card.getId()).thenReturn(1);

        cardDrawer.visit(card);

        verify(action).performDraw(card, rowsManager);
        verify(rowsManager).removeCard(1);
        verify(card).registerToTribe(player);
    }

    @Test
    @DisplayName("visit(CharacterCard) skips performDraw when action is null (PreEndOfRound context)")
    void visitCharacterCardWithNullActionSkipsPerformDraw() {
        CardDrawer drawerNoAction = new CardDrawer(player, rowsManager, null);
        CharacterCard card = mock(CharacterCard.class);
        when(card.getId()).thenReturn(2);

        drawerNoAction.visit(card);

        verify(rowsManager).removeCard(2);
        verify(card).registerToTribe(player);
    }

    @Test
    @DisplayName("visit(CharacterCard) triggers OnAcquire effects from tribe buildings")
    void visitCharacterCardTriggersOnAcquireEffects() {
        CharacterCard card = mock(CharacterCard.class);
        when(card.getId()).thenReturn(3);
        OnAcquireBuildingEffect effect = mock(OnAcquireBuildingEffect.class);
        when(tribe.getOnAcquireBuildingEffects()).thenReturn(List.of(effect));

        cardDrawer.visit(card);

        verify(effect).applyEffect(player);
    }

    @Test
    @DisplayName("visit(BuildingCard) succeeds when player has enough food")
    void visitBuildingCardWithEnoughFoodSucceeds() {
        BuildingCard card = mock(BuildingCard.class);
        when(card.getId()).thenReturn(10);
        when(card.getDiscountedCost(player)).thenReturn(3);
        when(player.getFood()).thenReturn(5);

        cardDrawer.visit(card);

        verify(player).removeFood(3);
        verify(card).registerToTribe(player);
        verify(rowsManager).removeCard(10);
    }

    @Test
    @DisplayName("visit(BuildingCard) skips performDraw when action is null (PreEndOfRoundPhase context)")
    void visitBuildingCardWithNullActionSkipsPerformDraw() {
        BuildingCard card = mock(BuildingCard.class);
        when(card.getId()).thenReturn(11);
        when(card.getDiscountedCost(player)).thenReturn(2);
        when(player.getFood()).thenReturn(5);

        CardDrawer drawerNoAction = new CardDrawer(player, rowsManager, null);
        drawerNoAction.visit(card);

        verify(rowsManager).removeCard(11);
        verify(player).removeFood(2);
        verify(card).registerToTribe(player);
    }

    @Test
    @DisplayName("visit(BuildingCard) throws when player has insufficient food")
    void visitBuildingCardWithInsufficientFoodThrows() {
        BuildingCard card = mock(BuildingCard.class);
        when(card.getDiscountedCost(player)).thenReturn(5);
        when(player.getFood()).thenReturn(2);

        assertThrows(IllegalStateException.class, () -> cardDrawer.visit(card));

        verify(player, never()).removeFood(anyInt());
        verify(card, never()).registerToTribe(any());
    }

    @Test
    @DisplayName("visit(EventCard) always throws")
    void visitEventCardThrows() {
        EventCard card = mock(EventCard.class);
        assertThrows(IllegalStateException.class, () -> cardDrawer.visit(card));
    }

    @Test
    @DisplayName("visit(SustenanceEventCard) always throws")
    void visitSustenanceEventCardThrows() {
        SustenanceEventCard card = mock(SustenanceEventCard.class);
        assertThrows(IllegalStateException.class, () -> cardDrawer.visit(card));
    }
}
