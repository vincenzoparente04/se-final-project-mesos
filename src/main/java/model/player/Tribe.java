package model.player;

import model.cards.buildingCards.buildingEffects.endGameEffects.EndGameBuildingEffect;
import model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects.OnAcquireBuildingEffect;
import model.cards.buildingCards.buildingEffects.onEventEffects.OnEventBuildingEffect;
import model.cards.buildingCards.BuildingCard;
import model.cards.characterCards.*;
import model.enums.InventionIcon;

import java.util.*;
import java.util.stream.Stream;


/**
 * Represents a player's tribe, acting as the in-game inventory of every
 * {@link CharacterCard} and {@link BuildingCard} acquired during the match.
 *
 * <p>Character cards are partitioned by type into dedicated collections
 * (artists, builders, gatherers, hunters, shamans) for fast type-specific
 * access; inventors are further grouped by {@link InventionIcon} to support
 * the scoring rule that rewards icon diversity. Building cards are kept in a
 * single list, while their effects are mirrored into three separate registries
 * — {@link OnAcquireBuildingEffect}, {@link OnEventBuildingEffect} and
 * {@link EndGameBuildingEffect} — populated through the {@code registerSelf}
 * callbacks of each effect at acquisition time.
 *
 * <p>The class also centralises all card-counting and end-game scoring
 * formulas, exposing them as query methods so that {@link Player} and the
 * end-of-game phase handler can compute prestige contributions without
 * inspecting the underlying collections directly.
 *
 * <p>All collection getters return unmodifiable views to preserve
 * encapsulation: mutations must go through the dedicated {@code add*} and
 * {@code register*} methods.
 */
public class Tribe {
    private final List<ArtistCard> artists = new ArrayList<>();
    private final List<BuilderCard> builders = new ArrayList<>();
    private final List<GathererCard> gatherers = new ArrayList<>();
    private final List<HunterCard> hunters = new ArrayList<>();
    private final Map<InventionIcon, List<InventorCard>> inventorsByIcon = new EnumMap<>(InventionIcon.class);
    private final List<ShamanCard> shamans = new ArrayList<>();

    private final List<BuildingCard> buildings = new ArrayList<>();
    private final List<EndGameBuildingEffect> endGameBuildingEffects = new ArrayList<>();
    private final List<OnAcquireBuildingEffect> onAcquireBuildingEffects = new ArrayList<>();
    private final List<OnEventBuildingEffect> onEventBuildingEffects = new ArrayList<>();

    public void addArtist(ArtistCard card) { artists.add(card); }
    public void addBuilder(BuilderCard card) { builders.add(card); }
    public void addGatherer(GathererCard card) { gatherers.add(card); }
    public void addHunter(HunterCard card) { hunters.add(card); }
    public void addShaman(ShamanCard card) { shamans.add(card); }
    public void addInventor(InventorCard card) {
        inventorsByIcon.computeIfAbsent(card.getInventionIcon(), k -> new ArrayList<>()).add(card);
    }

    public void addBuilding(BuildingCard card) {
        buildings.add(card);
    }

    // registration — called by the registerSelf of the effects
    public void registerOnEventEffect(OnEventBuildingEffect effect) {
        onEventBuildingEffects.add(effect);
    }
    public void registerOnAcquireEffect(OnAcquireBuildingEffect effect) {
        onAcquireBuildingEffects.add(effect);
    }
    public void registerEndGameEffect(EndGameBuildingEffect effect) {
        endGameBuildingEffects.add(effect);
    }

    public List<ArtistCard> getArtists() { return Collections.unmodifiableList(artists); }
    public List<BuilderCard> getBuilders() { return Collections.unmodifiableList(builders); }
    public List<GathererCard> getGatherers() { return Collections.unmodifiableList(gatherers); }
    public List<HunterCard> getHunters() { return Collections.unmodifiableList(hunters); }
    public List<ShamanCard> getShamans() { return Collections.unmodifiableList(shamans); }
    public List<BuildingCard> getBuildings() { return Collections.unmodifiableList(buildings); }
    public Map<InventionIcon, List<InventorCard>> getInventorsByIcon() {
        return Collections.unmodifiableMap(inventorsByIcon);
    }
    public List<OnEventBuildingEffect> getOnEventBuildingEffects() { return onEventBuildingEffects; }
    public List<OnAcquireBuildingEffect> getOnAcquireBuildingEffects() { return onAcquireBuildingEffects; }
    public List<EndGameBuildingEffect> getEndGameBuildingEffects() { return endGameBuildingEffects; }

    /**
     * Returns a flat list of all character cards in this tribe, regardless of type.
     * Used by the server layer to serialize tribe state without type-specific access.
     */
    public List<CharacterCard> getAllCharacters() {
        List<CharacterCard> all = new ArrayList<>();
        all.addAll(artists);
        all.addAll(builders);
        all.addAll(gatherers);
        all.addAll(hunters);
        all.addAll(shamans);
        inventorsByIcon.values().forEach(all::addAll);
        return Collections.unmodifiableList(all);
    }

    // query methods
    public int getHunterCount()         { return hunters.size(); }
    public int getArtistCount()         { return artists.size(); }
    public int getBuilderCount()        { return builders.size(); }
    public int getGathererCount()       { return gatherers.size(); }
    public int getShamanCount()         { return shamans.size(); }
    public int getInventorCount() {
        return inventorsByIcon.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public int getTotalCharacterCount() {
        return hunters.size() + builders.size() + shamans.size()
                + artists.size() + getInventorCount() + gatherers.size();
    }

    public int getTotalBuilderDiscount() {
        return builders.stream()
                .mapToInt(BuilderCard::getBuilderDiscount)
                .sum();
    }

    public int getTotalShamanStars() {
        return shamans.stream()
                .mapToInt(ShamanCard::getStarCount)
                .sum();
    }

    public int getDistinctInventionIcons() {
        return inventorsByIcon.size();
    }

    public int getTotalGatherersDiscount() {
        return getGathererCount() * 3;
    }


    // -- methods used for end game calculations --

    public int calculateBuildersEndGamePoints() {
        return builders.stream()
                .mapToInt(BuilderCard::getPrestigePoints)
                .sum();
    }

    public int calculateArtistEndGamePoints() {
        return (artists.size() / 2) * 10;
    }

    public int calculateInventorEndGamePoints() {
        return getInventorCount() * getDistinctInventionIcons();
    }

    public int calculateBuildingPrintedPoints() {
        return buildings.stream()
                .mapToInt(BuildingCard::getEndGamePoints)
                .sum();
    }

    public int countCompleteSets() {
        return Stream.of(
                artists.size(),
                builders.size(),
                gatherers.size(),
                hunters.size(),
                getInventorCount(),
                shamans.size()
        ).min(Integer::compareTo).orElse(0);
    }
}
