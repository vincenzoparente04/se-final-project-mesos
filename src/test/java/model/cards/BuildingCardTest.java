package model.cards;

import model.cards.buildingCards.BuildingCard;
import model.enums.Era;
import model.player.Player;
import model.player.Tribe;
import model.buildingEffects.BuildingEffect;
import model.rowsManager.CardVisitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class BuildingCardTest {
    @Mock private Player mockPlayer;
    @Mock private Tribe mockTribe;
    @Mock private BuildingEffect mockEffect;
    @Mock
    private CardVisitor mockVisitor;

    private BuildingCard buildingCard;

    private static final int ID = 1;
    private static final Era ERA = Era.ERA_I;
    private static final int PLAYER_COUNT = 3;
    private static final int FOOD_COST = 5;
    private static final int END_GAME_POINTS = 10;
    private static final String EFFECT_ID = "test_effect";
    private static final String IMAGE_PATH = "front.png";
    private static final String BACK_IMAGE_PATH = "back.png";

    @BeforeEach
    void setUp() {
        buildingCard = new BuildingCard(ID, ERA, PLAYER_COUNT, FOOD_COST, END_GAME_POINTS,
                mockEffect, EFFECT_ID, IMAGE_PATH, BACK_IMAGE_PATH);
    }

    @Test
    void testGettersReturnsCorrectValues() {
        assertEquals(FOOD_COST, buildingCard.getFoodCost(), "Food cost should match initialization");
        assertEquals(END_GAME_POINTS, buildingCard.getEndGamePoints(), "End game points should match initialization");
        assertEquals(mockEffect, buildingCard.getEffect(), "Effect should match initialization");
    }

    @Test
    @DisplayName("registerToTribe should add building to tribe and register effect")
    void testRegisterToTribeDelegatesCorrectly() {
        // Arrange
        when(mockPlayer.getTribe()).thenReturn(mockTribe);

        // Act
        buildingCard.registerToTribe(mockPlayer);

        // Assert
        verify(mockTribe, times(1)).addBuilding(buildingCard);
        verify(mockEffect, times(1)).registerSelf(mockPlayer);
    }

    @Test
    @DisplayName("getDiscountedCost should return full cost if no discount is available")
    void testGetDiscountedCost_NoDiscount() {
        when(mockPlayer.getTribe()).thenReturn(mockTribe);
        when(mockTribe.getTotalBuilderDiscount()).thenReturn(0);

        assertEquals(FOOD_COST, buildingCard.getDiscountedCost(mockPlayer),
                "Cost should be fully paid if there is no discount");
    }

    @Test
    @DisplayName("getDiscountedCost should return reduced cost when discount is available")
    void testGetDiscountedCost_PartialDiscount() {
        int partialDiscount = 2;
        when(mockPlayer.getTribe()).thenReturn(mockTribe);
        when(mockTribe.getTotalBuilderDiscount()).thenReturn(partialDiscount);

        assertEquals(FOOD_COST - partialDiscount, buildingCard.getDiscountedCost(mockPlayer),
                "Cost should be reduced by the exact discount amount");
    }

    @Test
    @DisplayName("getDiscountedCost should not return negative cost even if discount exceeds cost")
    void testGetDiscountedCost_DiscountExceedsCost() {
        int hugeDiscount = 10; // Greater than FOOD_COST (5)
        when(mockPlayer.getTribe()).thenReturn(mockTribe);
        when(mockTribe.getTotalBuilderDiscount()).thenReturn(hugeDiscount);

        assertEquals(0, buildingCard.getDiscountedCost(mockPlayer),
                "Cost cannot be less than zero, even with excess builders");
    }
}