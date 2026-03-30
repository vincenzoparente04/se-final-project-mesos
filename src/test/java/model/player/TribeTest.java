package model.player;

import model.buildingEffects.EndGameEffects.EndGameBuildingEffect;
import model.buildingEffects.OnCharacterAcquiredEffects.OnAcquireBuildingEffect;
import model.buildingEffects.OnEventEffects.OnEventBuildingEffect;
import model.cards.buildingCards.BuildingCard;
import model.cards.charachterCards.ArtistCard;
import model.cards.charachterCards.BuilderCard;
import model.cards.charachterCards.GathererCard;
import model.cards.charachterCards.HunterCard;
import model.cards.charachterCards.InventorCard;
import model.cards.charachterCards.ShamanCard;
import model.enums.Era;
import model.enums.InventionIcon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Test for the Tribe class, which manages the character cards of a player and their related logic.
// The CharacterCard created are not real cards, but just dummy implementations to test the logic of the tribe class without depending on the actual card implementations.

@DisplayName("Tribe Tests")
class TribeTest {

	private Tribe tribe;

	@BeforeEach
	void setUp() {
		tribe = new Tribe();
	}

	@Test
	@DisplayName("Initial counts are zero")
	void initialCountsAreZero() {
		assertEquals(0, tribe.getArtistCount());
		assertEquals(0, tribe.getBuilderCount());
		assertEquals(0, tribe.getGathererCount());
		assertEquals(0, tribe.getHunterCount());
		assertEquals(0, tribe.getInventorCount());
		assertEquals(0, tribe.getShamanCount());
		assertEquals(0, tribe.getTotalCharacterCount());
		assertEquals(0, tribe.countCompleteSets());
	}

	@Test
	@DisplayName("Getter lists are unmodifiable")
	void listsAreTrulyUnmodifiable() {
		tribe.addArtist(new ArtistCard(1, Era.ERA_I, 2));
		List<ArtistCard> artists = tribe.getArtists();

		assertThrows(UnsupportedOperationException.class, () -> {
			artists.add(new ArtistCard(2, Era.ERA_I, 2));
		}, "Should launch an exception if the getter tries to modify the list");
	}

	@Test
	@DisplayName("add* increment the correct count")
	void addMethodsIncrementCounts() {
		tribe.addArtist(new ArtistCard(1, Era.ERA_I, 2));
		tribe.addBuilder(new BuilderCard(2, Era.ERA_I, 2, 1, 3));
		tribe.addGatherer(new GathererCard(3, Era.ERA_I, 2));
		tribe.addHunter(new HunterCard(4, Era.ERA_I, 2, false));
		tribe.addInventor(new InventorCard(5, Era.ERA_I, 2, InventionIcon.ICON_1));
		tribe.addShaman(new ShamanCard(6, Era.ERA_I, 2, 2));

        // Verifies that each card was added to the correct list and that the counts reflect the additions
		assertEquals(1, tribe.getArtistCount());
		assertEquals(1, tribe.getBuilderCount());
		assertEquals(1, tribe.getGathererCount());
		assertEquals(1, tribe.getHunterCount());
		assertEquals(1, tribe.getInventorCount());
		assertEquals(1, tribe.getShamanCount());

        // Verifies that the total character count is the sum of all individual counts
		assertEquals(6, tribe.getTotalCharacterCount());

        // Verifies that the number of complete sets is correctly calculated based on the minimum count among the character types (in this case, 1)
		assertEquals(1, tribe.countCompleteSets());
	}

	@Test
	@DisplayName("Total builder discount is correctly summed")
	void totalBuilderDiscountIsSummed() {
		tribe.addBuilder(new BuilderCard(1, Era.ERA_I, 2, 1, 2));
		tribe.addBuilder(new BuilderCard(2, Era.ERA_I, 2, 3, 4));
		tribe.addBuilder(new BuilderCard(3, Era.ERA_II, 2, 2, 1));

		assertEquals(6, tribe.getTotalBuilderDiscount());
	}

	@Test
	@DisplayName("Total shaman stars are correctly summed")
	void totalShamanStarsAreSummed() {
		tribe.addShaman(new ShamanCard(1, Era.ERA_I, 2, 2));
		tribe.addShaman(new ShamanCard(2, Era.ERA_II, 2, 3));

		assertEquals(5, tribe.getTotalShamanStars());
	}

	@Test
	@DisplayName("Distinct invention icons are correctly counted")
	void distinctInventionIconsAreCounted() {
		tribe.addInventor(new InventorCard(1, Era.ERA_I, 2, InventionIcon.ICON_1));
		tribe.addInventor(new InventorCard(2, Era.ERA_I, 2, InventionIcon.ICON_2));
		tribe.addInventor(new InventorCard(3, Era.ERA_II, 2, InventionIcon.ICON_1));

		assertEquals(2, tribe.getDistinctInventionIcons());
	}

	@Test
	@DisplayName("Inventor points with duplicate icons")
	void inventorPointsWithDuplicateIcons() {
		// 2 inventori, entrambi con ICON_1
		tribe.addInventor(new InventorCard(1, Era.ERA_I, 2, InventionIcon.ICON_1));
		tribe.addInventor(new InventorCard(2, Era.ERA_I, 2, InventionIcon.ICON_1));

		// Count (2) * Distinct Icons (1) = 2
		assertEquals(2, tribe.calculateInventorEndGamePoints());
	}

	@Test
	@DisplayName("Inventors map is unmodifiable")
	void inventorsMapIsUnmodifiable() {
		assertThrows(UnsupportedOperationException.class, () -> {
			tribe.getInventorsByIcon().put(InventionIcon.ICON_1, new ArrayList<>());
		});
	}

	@Test
	@DisplayName("Gatherer discount = number of gatherers * 3")
	void totalGathererDiscountDependsOnCount() {
		tribe.addGatherer(new GathererCard(1, Era.ERA_I, 2));
		tribe.addGatherer(new GathererCard(2, Era.ERA_II, 2));
		tribe.addGatherer(new GathererCard(3, Era.ERA_III, 2));

		assertEquals(9, tribe.getTotalGatherersDiscount());
	}

	@Test
	@DisplayName("End game points for builders is the sum of their points")
	void builderEndGamePointsAreSummed() {
		tribe.addBuilder(new BuilderCard(1, Era.ERA_I, 2, 0, 2));
		tribe.addBuilder(new BuilderCard(2, Era.ERA_I, 2, 0, 5));

		assertEquals(7, tribe.calculateBuildersEndGamePoints());
	}

	@Test
	@DisplayName("Artist points: 10 points per pair of artists")
	void artistEndGamePointsByPairs() {
		tribe.addArtist(new ArtistCard(1, Era.ERA_I, 2));
		tribe.addArtist(new ArtistCard(2, Era.ERA_I, 2));
		tribe.addArtist(new ArtistCard(3, Era.ERA_II, 2));
		tribe.addArtist(new ArtistCard(4, Era.ERA_II, 2));
		tribe.addArtist(new ArtistCard(5, Era.ERA_III, 2));

		assertEquals(20, tribe.calculateArtistEndGamePoints());
	}

	@Test
	@DisplayName("Inventor points: number of inventors * distinct icons")
	void inventorEndGamePointsUseDistinctIcons() {
		tribe.addInventor(new InventorCard(1, Era.ERA_I, 2, InventionIcon.ICON_1));
		tribe.addInventor(new InventorCard(2, Era.ERA_I, 2, InventionIcon.ICON_2));
		tribe.addInventor(new InventorCard(3, Era.ERA_II, 2, InventionIcon.ICON_2));

		assertEquals(6, tribe.calculateInventorEndGamePoints());
	}

	@Test
	@DisplayName("Set completi: minimo tra i sei tipi")
	void completeSetsUseMinimumCharacterTypeCount() {
		tribe.addArtist(new ArtistCard(1, Era.ERA_I, 2));
		tribe.addArtist(new ArtistCard(2, Era.ERA_I, 2));

		tribe.addBuilder(new BuilderCard(3, Era.ERA_I, 2, 0, 1));
		tribe.addBuilder(new BuilderCard(4, Era.ERA_I, 2, 0, 1));

		tribe.addGatherer(new GathererCard(5, Era.ERA_I, 2));
		tribe.addGatherer(new GathererCard(6, Era.ERA_I, 2));

		tribe.addHunter(new HunterCard(7, Era.ERA_I, 2, false));

		tribe.addInventor(new InventorCard(8, Era.ERA_I, 2, InventionIcon.ICON_1));
		tribe.addInventor(new InventorCard(9, Era.ERA_I, 2, InventionIcon.ICON_2));
		tribe.addInventor(new InventorCard(10, Era.ERA_I, 2, InventionIcon.ICON_3));

		tribe.addShaman(new ShamanCard(11, Era.ERA_I, 2, 1));
		tribe.addShaman(new ShamanCard(12, Era.ERA_I, 2, 1));

		assertEquals(1, tribe.countCompleteSets());
	}

	//BuildingCard tests using mocks 
	@Test
	@DisplayName("Building printed points: sum of all building points")
	void buildingPrintedPointsAreSummed() {
		BuildingCard mock1 = mock(BuildingCard.class);
		BuildingCard mock2 = mock(BuildingCard.class);
		BuildingCard mock3 = mock(BuildingCard.class);

		when(mock1.getEndGamePoints()).thenReturn(5);
		when(mock2.getEndGamePoints()).thenReturn(8);
		when(mock3.getEndGamePoints()).thenReturn(3);

		tribe.addBuilding(mock1);
		tribe.addBuilding(mock2);
		tribe.addBuilding(mock3);

		assertEquals(16, tribe.calculateBuildingPrintedPoints());
	}

	@Test
	@DisplayName("Building printed points: no buildings yields zero points")
	void buildingPrintedPointsWithNoBuildings() {
		assertEquals(0, tribe.calculateBuildingPrintedPoints());
	}

	@Test
	@DisplayName("Building printed points: single building")
	void buildingPrintedPointsWithSingleBuilding() {
		BuildingCard mockBuilding = mock(BuildingCard.class);
		when(mockBuilding.getEndGamePoints()).thenReturn(12);

		tribe.addBuilding(mockBuilding);

		assertEquals(12, tribe.calculateBuildingPrintedPoints());
	}

	// ===== EDGE CASES FOR ARTISTS =====
	@Test
	@DisplayName("Artist points: single artist yields zero points")
	void artistEndGamePointsSingleArtist() {
		tribe.addArtist(new ArtistCard(1, Era.ERA_I, 2));

		assertEquals(0, tribe.calculateArtistEndGamePoints());
	}

	@Test
	@DisplayName("Artist points: no artists yields zero points")
	void artistEndGamePointsNoArtists() {
		assertEquals(0, tribe.calculateArtistEndGamePoints());
	}

	// ===== EDGE CASES FOR BUILDERS =====
	@Test
	@DisplayName("Builder discount: no builders yields zero discount")
	void totalBuilderDiscountNoBuilders() {
		assertEquals(0, tribe.getTotalBuilderDiscount());
	}

	@Test
	@DisplayName("Builder discount: single builder")
	void totalBuilderDiscountSingleBuilder() {
		tribe.addBuilder(new BuilderCard(1, Era.ERA_I, 2, 5, 10));

		assertEquals(5, tribe.getTotalBuilderDiscount());
	}

	@Test
	@DisplayName("Builder points: no builders yields zero points")
	void builderEndGamePointsNoBuilders() {
		assertEquals(0, tribe.calculateBuildersEndGamePoints());
	}

	// ===== EDGE CASES FOR GATHERERS =====
	@Test
	@DisplayName("Gatherer discount: no gatherers yields zero discount")
	void totalGathererDiscountNoGatherers() {
		assertEquals(0, tribe.getTotalGatherersDiscount());
	}

	@Test
	@DisplayName("Gatherer discount: single gatherer yields 3")
	void totalGathererDiscountSingleGatherer() {
		tribe.addGatherer(new GathererCard(1, Era.ERA_I, 2));

		assertEquals(3, tribe.getTotalGatherersDiscount());
	}

	// ===== EDGE CASES FOR SHAMAN =====
	@Test
	@DisplayName("Shaman stars: no shamans yields zero stars")
	void totalShamanStarsNoShamans() {
		assertEquals(0, tribe.getTotalShamanStars());
	}

	@Test
	@DisplayName("Shaman stars: single shaman")
	void totalShamanStarsSingleShaman() {
		tribe.addShaman(new ShamanCard(1, Era.ERA_I, 2, 4));

		assertEquals(4, tribe.getTotalShamanStars());
	}

	// ===== EDGE CASES FOR INVENTORS =====
	@Test
	@DisplayName("Inventor points: no inventors yields zero points")
	void inventorEndGamePointsNoInventors() {
		assertEquals(0, tribe.calculateInventorEndGamePoints());
	}

	@Test
	@DisplayName("Inventor points: single inventor with single icon")
	void inventorEndGamePointsSingleInventor() {
		tribe.addInventor(new InventorCard(1, Era.ERA_I, 2, InventionIcon.ICON_1));

		assertEquals(1, tribe.calculateInventorEndGamePoints());
	}

	@Test
	@DisplayName("Invention icons: no inventors yields zero icons")
	void distinctInventionIconsNoInventors() {
		assertEquals(0, tribe.getDistinctInventionIcons());
	}

	@Test
	@DisplayName("Invention icons: all inventors with same icon")
	void distinctInventionIconsAllSame() {
		tribe.addInventor(new InventorCard(1, Era.ERA_I, 2, InventionIcon.ICON_1));
		tribe.addInventor(new InventorCard(2, Era.ERA_I, 2, InventionIcon.ICON_1));
		tribe.addInventor(new InventorCard(3, Era.ERA_I, 2, InventionIcon.ICON_1));

		assertEquals(1, tribe.getDistinctInventionIcons());
	}

	// ===== EDGE CASES FOR COMPLETE SETS =====
	@Test
	@DisplayName("Complete sets: empty tribe yields zero sets")
	void completeSetsEmptyTribe() {
		assertEquals(0, tribe.countCompleteSets());
	}

	@Test
	@DisplayName("Complete sets: only one type of character yields zero sets")
	void completeSetsOnlyOneType() {
		tribe.addArtist(new ArtistCard(1, Era.ERA_I, 2));
		tribe.addArtist(new ArtistCard(2, Era.ERA_I, 2));
		tribe.addArtist(new ArtistCard(3, Era.ERA_I, 2));

		assertEquals(0, tribe.countCompleteSets());
	}

	@Test
	@DisplayName("Complete sets: four complete sets with four of each type")
	void completeSetsManyComplete() {
		// Add 4 of each type
		for (int i = 0; i < 4; i++) {
			tribe.addArtist(new ArtistCard(100 + i, Era.ERA_I, 2));
			tribe.addBuilder(new BuilderCard(200 + i, Era.ERA_I, 2, 0, 1));
			tribe.addGatherer(new GathererCard(300 + i, Era.ERA_I, 2));
			tribe.addHunter(new HunterCard(400 + i, Era.ERA_I, 2, false));
			tribe.addInventor(new InventorCard(500 + i, Era.ERA_I, 2, InventionIcon.ICON_1));
			tribe.addShaman(new ShamanCard(600 + i, Era.ERA_I, 2, 1));
		}

		assertEquals(4, tribe.countCompleteSets());
	}

	@Test
	@DisplayName("Complete sets: varied counts (minimum is limiting)")
	void completeSetsVariedCounts() {
		tribe.addArtist(new ArtistCard(1, Era.ERA_I, 2));
		tribe.addArtist(new ArtistCard(2, Era.ERA_I, 2));
		tribe.addArtist(new ArtistCard(3, Era.ERA_I, 2));

		tribe.addBuilder(new BuilderCard(4, Era.ERA_I, 2, 0, 1));

		tribe.addGatherer(new GathererCard(5, Era.ERA_I, 2));
		tribe.addGatherer(new GathererCard(6, Era.ERA_I, 2));

		tribe.addHunter(new HunterCard(7, Era.ERA_I, 2, false));
		tribe.addHunter(new HunterCard(8, Era.ERA_I, 2, false));
		tribe.addHunter(new HunterCard(9, Era.ERA_I, 2, false));
		tribe.addHunter(new HunterCard(10, Era.ERA_I, 2, false));

		tribe.addInventor(new InventorCard(11, Era.ERA_I, 2, InventionIcon.ICON_1));
		tribe.addInventor(new InventorCard(12, Era.ERA_I, 2, InventionIcon.ICON_2));

		tribe.addShaman(new ShamanCard(13, Era.ERA_I, 2, 1));

		// Minimum is Builder with 1, so 1 complete set
		assertEquals(1, tribe.countCompleteSets());
	}

	// ===== EDGE CASES FOR TOTAL CHARACTER COUNT =====
	@Test
	@DisplayName("Total character count: empty tribe")
	void totalCharacterCountEmptyTribe() {
		assertEquals(0, tribe.getTotalCharacterCount());
	}

	@Test
	@DisplayName("Total character count: single character")
	void totalCharacterCountSingleCharacter() {
		tribe.addArtist(new ArtistCard(1, Era.ERA_I, 2));

		assertEquals(1, tribe.getTotalCharacterCount());
	}

	@Test
	@DisplayName("Registration of building effects")
	void registrationOfEffectsWorks() {
		OnEventBuildingEffect mockEvent = mock(OnEventBuildingEffect.class);
		OnAcquireBuildingEffect mockAcquire = mock(OnAcquireBuildingEffect.class);
		EndGameBuildingEffect mockEndGame = mock(EndGameBuildingEffect.class);

		tribe.registerOnEventEffect(mockEvent);
		tribe.registerOnAcquireEffect(mockAcquire);
		tribe.registerEndGameEffect(mockEndGame);

		assertEquals(1, tribe.getOnEventBuildingEffects().size());
		assertTrue(tribe.getOnEventBuildingEffects().contains(mockEvent));
		assertEquals(1, tribe.getOnAcquireBuildingEffects().size());
		assertEquals(1, tribe.getEndGameBuildingEffects().size());
	}

}
