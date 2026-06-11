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

    /**
     * Adds an inventor card to the tribe, categorizing it by its invention icon.
     *
     * @param card the inventor card to add
     */
    public void addInventor(InventorCard card) {
        inventorsByIcon.computeIfAbsent(card.getInventionIcon(), k -> new ArrayList<>()).add(card);
    }

    public void addBuilding(BuildingCard card) {
        buildings.add(card);
    }

    /**
     * Registers an on-event building effect to be triggered when events are resolved.
     * Called by the registerSelf of the effects during acquisition of a building card with on-event effects.
     *
     * @param effect the on-event building effect to register
     */
    public void registerOnEventEffect(OnEventBuildingEffect effect) {
        onEventBuildingEffects.add(effect);
    }

    /**
     * Registers an on-acquire building effect to be triggered when a character is acquired.
     *
     * @param effect the on-acquire building effect to register
     */
    public void registerOnAcquireEffect(OnAcquireBuildingEffect effect) {
        onAcquireBuildingEffects.add(effect);
    }

    /**
     * Registers an end-game building effect to be applied during end-game scoring.
     *
     * @param effect the end-game building effect to register
     */
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

    /**
     * Returns all on-event building effects registered in this tribe.
     *
     * @return a list of on-event building effects
     */
    public List<OnEventBuildingEffect> getOnEventBuildingEffects() { 
        return onEventBuildingEffects; 
    }

    /**
     * Returns all on-acquire building effects registered in this tribe.
     *
     * @return a list of on-acquire building effects
     */
    public List<OnAcquireBuildingEffect> getOnAcquireBuildingEffects() { 
        return onAcquireBuildingEffects; 
    }

    /**
     * Returns all end-game building effects registered in this tribe.
     *
     * @return a list of end-game building effects
     */
    public List<EndGameBuildingEffect> getEndGameBuildingEffects() { 
        return endGameBuildingEffects; 
    }

    /**
     * Returns a flat list of all character cards in this tribe, regardless of type.
     * 
     * This is used by the server layer to serialize entire tribe state without
     * requiring type-specific access.
     *
     * @return an unmodifiable list of all character cards
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

    /**
     * Returns the total number of character cards in this tribe.
     *
     * @return the count of all character cards (hunters, builders, shamans, artists, inventors, gatherers)
     */
    public int getTotalCharacterCount() {
        return hunters.size() + builders.size() + shamans.size()
                + artists.size() + getInventorCount() + gatherers.size();
    }

    /**
     * Returns the combined builder discount from all builder cards.
     *
     * @return the total builder discount
     */
    public int getTotalBuilderDiscount() {
        return builders.stream()
                .mapToInt(BuilderCard::getBuilderDiscount)
                .sum();
    }

    /**
     * Returns the combined star count from all shaman cards.
     *
     * @return the total number of stars from shamans
     */
    public int getTotalShamanStars() {
        return shamans.stream()
                .mapToInt(ShamanCard::getStarCount)
                .sum();
    }

    /**
     * Returns the number of distinct invention icons represented in this tribe.
     *
     * @return the count of unique invention icons
     */
    public int getDistinctInventionIcons() {
        return inventorsByIcon.size();
    }

    /**
     * Returns the total gatherer discount (3 prestige per gatherer card).
     *
     * @return the total gatherer discount
     */
    public int getTotalGatherersDiscount() {
        return getGathererCount() * 3;
    }


    // -- methods used for end game calculations --

    /**
     * Calculates the end-game prestige points contributed by builder cards.
     *
     * @return the sum of prestige points from all builder cards
     */
    public int calculateBuildersEndGamePoints() {
        return builders.stream()
                .mapToInt(BuilderCard::getPrestigePoints)
                .sum();
    }

    /**
     * Calculates the end-game prestige points contributed by artist cards.
     * 
     * Award is calculated as (artist count / 2) * 10.
     *
     * @return the calculated prestige points from artists
     */
    public int calculateArtistEndGamePoints() {
        return (artists.size() / 2) * 10;
    }

    /**
     * Calculates the end-game prestige points contributed by inventor cards.
     * 
     * Award is calculated as (inventor count) * (distinct invention icons).
     *
     * @return the calculated prestige points from inventors
     */
    public int calculateInventorEndGamePoints() {
        return getInventorCount() * getDistinctInventionIcons();
    }

    /**
     * Calculates the end-game prestige points from all building cards.
     *
     * @return the sum of end-game points printed on all building cards
     */
    public int calculateBuildingPrintedPoints() {
        return buildings.stream()
                .mapToInt(BuildingCard::getEndGamePoints)
                .sum();
    }

    /**
     * Counts the number of complete sets of character types.
     * 
     * A complete set contains one card of each character type (artist, builder, gatherer,
     * hunter, inventor, shaman). Returns the minimum count across all types, representing
     * the maximum number of complete sets that can be formed.
     *
     * @return the number of complete character sets
     */
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
