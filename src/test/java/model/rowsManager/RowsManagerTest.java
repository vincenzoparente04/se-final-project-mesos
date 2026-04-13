package model.rowsManager;

import model.cards.Card;
import model.cards.TribeCard;
import model.cards.buildingCards.BuildingCard;
import model.player.Player;
import model.rowsManager.deck.BuildingDeck;
import model.rowsManager.deck.TribeDeck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class  RowsManagerTest {

    private RowsManager rowsManager;

    private List<TribeCard> topRowTribe;
    private List<TribeCard> bottomRowTribe;
    private List<BuildingCard> topRowBuilding;
    private List<BuildingCard> bottomRowBuilding;

    private TribeDeck tribeDeck;
    private BuildingDeck buildingDeckEraI;
    private BuildingDeck buildingDeckEraII;
    private BuildingDeck buildingDeckEraIII;

    @BeforeEach
    void setUp() throws Exception {
        rowsManager = new RowsManager();

        topRowTribe = new ArrayList<>();
        bottomRowTribe = new ArrayList<>();
        topRowBuilding = new ArrayList<>();
        bottomRowBuilding = new ArrayList<>();

        tribeDeck = mock(TribeDeck.class);
        buildingDeckEraI = mock(BuildingDeck.class);
        buildingDeckEraII = mock(BuildingDeck.class);
        buildingDeckEraIII = mock(BuildingDeck.class);

        setField(rowsManager, "topRowTribe", topRowTribe);
        setField(rowsManager, "bottomRowTribe", bottomRowTribe);
        setField(rowsManager, "topRowBuilding", topRowBuilding);
        setField(rowsManager, "bottomRowBuilding", bottomRowBuilding);

        setField(rowsManager, "tribeDeck", tribeDeck);
        setField(rowsManager, "buildingDeckEraI", buildingDeckEraI);
        setField(rowsManager, "buildingDeckEraII", buildingDeckEraII);
        setField(rowsManager, "buildingDeckEraIII", buildingDeckEraIII);
    }

    @Test
    @DisplayName("topRowContainsCard returns true when tribe card is in top row")
    void topRowContainsCardReturnsTrueForTopRowTribeCard() {
        TribeCard tribeCard = mock(TribeCard.class);
        when(tribeCard.getId()).thenReturn(10);
        topRowTribe.add(tribeCard);

        assertTrue(rowsManager.topRowContainsCard(10));
    }

    @Test
    @DisplayName("topRowContainsCard returns true when building card is in top row")
    void topRowContainsCardReturnsTrueForTopRowBuildingCard() {
        BuildingCard buildingCard = mock(BuildingCard.class);
        when(buildingCard.getId()).thenReturn(20);
        topRowBuilding.add(buildingCard);

        assertTrue(rowsManager.topRowContainsCard(20));
    }

    @Test
    @DisplayName("topRowContainsCard returns false when card is absent")
    void topRowContainsCardReturnsFalseWhenAbsent() {
        assertFalse(rowsManager.topRowContainsCard(999));
    }

    @Test
    @DisplayName("bottomRowContainsCard returns true when tribe card is in bottom row")
    void bottomRowContainsCardReturnsTrueForBottomRowTribeCard() {
        TribeCard tribeCard = mock(TribeCard.class);
        when(tribeCard.getId()).thenReturn(30);
        bottomRowTribe.add(tribeCard);

        assertTrue(rowsManager.bottomRowContainsCard(30));
    }

    @Test
    @DisplayName("bottomRowContainsCard returns true when building card is in bottom row")
    void bottomRowContainsCardReturnsTrueForBottomRowBuildingCard() {
        BuildingCard buildingCard = mock(BuildingCard.class);
        when(buildingCard.getId()).thenReturn(40);
        bottomRowBuilding.add(buildingCard);

        assertTrue(rowsManager.bottomRowContainsCard(40));
    }

    @Test
    @DisplayName("bottomRowContainsCard returns false when card is absent")
    void bottomRowContainsCardReturnsFalseWhenAbsent() {
        assertFalse(rowsManager.bottomRowContainsCard(999));
    }

    @Test
    @DisplayName("findCardById finds card in top row tribe first")
    void findCardByIdFindsCardInTopRowTribe() {
        TribeCard tribeCard = mock(TribeCard.class);
        when(tribeCard.getId()).thenReturn(1);
        topRowTribe.add(tribeCard);

        Card result = rowsManager.findCardById(1);

        assertSame(tribeCard, result);
    }

    @Test
    @DisplayName("findCardById returns null when card does not exist")
    void findCardByIdReturnsNullWhenMissing() {
        assertNull(rowsManager.findCardById(404));
    }

    @Test
    @DisplayName("removeCard removes matching card from top tribe row")
    void removeCardRemovesFromTopTribeRow() {
        TribeCard tribeCard = mock(TribeCard.class);
        when(tribeCard.getId()).thenReturn(11);
        topRowTribe.add(tribeCard);

        rowsManager.removeCard(11);

        assertFalse(rowsManager.topRowContainsCard(11));
        assertTrue(topRowTribe.isEmpty());
    }

    @Test
    @DisplayName("removeCard removes matching card from all rows if duplicated id exists")
    void removeCardRemovesMatchingIdFromAllRows() {
        TribeCard topTribe = mock(TribeCard.class);
        TribeCard bottomTribe = mock(TribeCard.class);
        BuildingCard topBuilding = mock(BuildingCard.class);
        BuildingCard bottomBuilding = mock(BuildingCard.class);

        when(topTribe.getId()).thenReturn(77);
        when(bottomTribe.getId()).thenReturn(77);
        when(topBuilding.getId()).thenReturn(77);
        when(bottomBuilding.getId()).thenReturn(77);

        topRowTribe.add(topTribe);
        bottomRowTribe.add(bottomTribe);
        topRowBuilding.add(topBuilding);
        bottomRowBuilding.add(bottomBuilding);

        rowsManager.removeCard(77);

        assertFalse(rowsManager.topRowContainsCard(77));
        assertFalse(rowsManager.bottomRowContainsCard(77));
        assertNull(rowsManager.findCardById(77));
    }

    @Test
    @DisplayName("getAllCardsOnBoard returns bottom row cards first then top row cards")
    void getAllCardsOnBoardReturnsBottomThenTopOrder() {
        TribeCard bottom1 = mock(TribeCard.class);
        TribeCard bottom2 = mock(TribeCard.class);
        TribeCard top1 = mock(TribeCard.class);

        bottomRowTribe.add(bottom1);
        bottomRowTribe.add(bottom2);
        topRowTribe.add(top1);

        List<TribeCard> result = rowsManager.getAllTribeCardsOnBoard();
        //TODO: what about the other cards (normal not tribe)? before it was called as getAllCardsOnBoard but list was <TribeCard>

        assertEquals(3, result.size());
        assertSame(bottom1, result.get(0));
        assertSame(bottom2, result.get(1));
        assertSame(top1, result.get(2));
    }

    @Test
    @DisplayName("getAllCardsOnBoard returns a new list and not internal storage")
    void getAllCardsOnBoardReturnsDefensiveCopy() {
        TribeCard bottom = mock(TribeCard.class);
        bottomRowTribe.add(bottom);

        List<TribeCard> result = rowsManager.getAllTribeCardsOnBoard();
        //TODO: what about the other cards (normal not tribe)?
        result.clear();

        assertEquals(1, bottomRowTribe.size());
        assertSame(bottom, bottomRowTribe.get(0));
    }

    @Test
    @DisplayName("endRound clears bottom row, moves top tribe cards to bottom, then draws playerCount+4 cards")
    void endRoundMovesRowsAndDrawsNewTopCards() {
        TribeCard oldBottom = mock(TribeCard.class);
        TribeCard top1 = mock(TribeCard.class);
        TribeCard top2 = mock(TribeCard.class);
        TribeCard drawn1 = mock(TribeCard.class);
        TribeCard drawn2 = mock(TribeCard.class);
        TribeCard drawn3 = mock(TribeCard.class);
        TribeCard drawn4 = mock(TribeCard.class);
        TribeCard drawn5 = mock(TribeCard.class);
        TribeCard drawn6 = mock(TribeCard.class);

        bottomRowTribe.add(oldBottom);
        topRowTribe.add(top1);
        topRowTribe.add(top2);

        when(tribeDeck.drawMultiple(6)).thenReturn(List.of(drawn1, drawn2, drawn3, drawn4, drawn5, drawn6));

        rowsManager.endRound(2);

        assertEquals(List.of(top1, top2), bottomRowTribe);

        // ###assert modificata poichè la classe rows manager è staat modificata
        assertEquals(List.of(drawn1, drawn2, drawn3, drawn4, drawn5, drawn6), topRowTribe);

        verify(tribeDeck).drawMultiple(6);
    }

    @Test
    @DisplayName("endRound on empty top row still draws playerCount+4 cards")
    void endRoundOnEmptyTopRowStillDrawsCards() {
        TribeCard drawn1 = mock(TribeCard.class);
        TribeCard drawn2 = mock(TribeCard.class);
        TribeCard drawn3 = mock(TribeCard.class);
        TribeCard drawn4 = mock(TribeCard.class);
        TribeCard drawn5 = mock(TribeCard.class);

        when(tribeDeck.drawMultiple(5)).thenReturn(List.of(drawn1, drawn2, drawn3, drawn4, drawn5));

        rowsManager.endRound(1);

        assertTrue(bottomRowTribe.isEmpty());
        assertEquals(List.of(drawn1, drawn2, drawn3, drawn4, drawn5), topRowTribe);
        verify(tribeDeck).drawMultiple(5);
    }

    @Test
    @DisplayName("changeEra moves top buildings to bottom and loads Era II when Era II deck is not empty")
    void changeEraLoadsEraIIWhenAvailable() {
        BuildingCard oldBottom = mock(BuildingCard.class);
        BuildingCard top1 = mock(BuildingCard.class);
        BuildingCard era2a = mock(BuildingCard.class);
        BuildingCard era2b = mock(BuildingCard.class);

        bottomRowBuilding.add(oldBottom);
        topRowBuilding.add(top1);

        when(buildingDeckEraII.isEmpty()).thenReturn(false);
        when(buildingDeckEraII.drawAll()).thenReturn(List.of(era2a, era2b));

        rowsManager.changeEra();

        assertEquals(List.of(top1), bottomRowBuilding);

        // ###rowsManager ora svuota le carte
        assertEquals(List.of(era2a, era2b), topRowBuilding);

        verify(buildingDeckEraII).isEmpty();
        verify(buildingDeckEraII).drawAll();
        verify(buildingDeckEraIII, never()).drawAll();
    }

    @Test
    @DisplayName("changeEra loads Era III when Era II deck is empty")
    void changeEraLoadsEraIIIWhenEraIIDeckIsEmpty() {
        BuildingCard top1 = mock(BuildingCard.class);
        BuildingCard era3a = mock(BuildingCard.class);

        topRowBuilding.add(top1);

        when(buildingDeckEraII.isEmpty()).thenReturn(true);
        when(buildingDeckEraIII.drawAll()).thenReturn(List.of(era3a));

        rowsManager.changeEra();

        assertEquals(List.of(top1), bottomRowBuilding);
        assertEquals(List.of(era3a), topRowBuilding);

        verify(buildingDeckEraII).isEmpty();
        verify(buildingDeckEraIII).drawAll();
        verify(buildingDeckEraII, never()).drawAll();
    }


    @Test
    @DisplayName("getTribeDeck returns internal tribe deck")
    void getTribeDeckReturnsInjectedDeck() {
        assertSame(tribeDeck, rowsManager.getTribeDeck());
    }

    @Test
    @DisplayName("resolveEvents delegates to resolver without throwing when bottom row has no events")
    void resolveEventsDoesNotThrowWithEmptyBottomRow() {
        assertDoesNotThrow(() -> rowsManager.resolveEvents(List.of(mock(Player.class))));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}