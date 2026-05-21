package model.player;

import model.cards.buildingCards.buildingEffects.endGameEffects.EndGameBuildingEffect;
import model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects.OnAcquireBuildingEffect;
import model.cards.buildingCards.buildingEffects.onEventEffects.OnEventBuildingEffect;
import model.cards.buildingCards.BuildingCard;
import model.cards.characterCards.*;
import model.enums.InventionIcon;

import java.util.*;
import java.util.stream.Stream;

public class Tribe {
    // 6 liste diverse per gestire i vari tipi di character card, più una lista per i building card
    private final List<ArtistCard> artists = new ArrayList<>();
    private final List<BuilderCard> builders = new ArrayList<>();
    private final List<GathererCard> gatherers = new ArrayList<>();
    private final List<HunterCard> hunters = new ArrayList<>();
    private final Map<InventionIcon, List<InventorCard>> inventorsByIcon = new EnumMap<>(InventionIcon.class);
    private final List<ShamanCard> shamans = new ArrayList<>();

    private final List<BuildingCard> buildings = new ArrayList<>();

    // 3 liste per effetti dei buildings
    private final List<EndGameBuildingEffect> endGameBuildingEffects = new ArrayList<>();
    private final List<OnAcquireBuildingEffect> onAcquireBuildingEffects = new ArrayList<>();
    private final List<OnEventBuildingEffect> onEventBuildingEffects = new ArrayList<>();

    // metodi per aggiungere le carte alle varie liste
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

    // registrazione — chiamati dai registerSelf degli effetti
    public void registerOnEventEffect(OnEventBuildingEffect effect) {
        onEventBuildingEffects.add(effect);
    }
    public void registerOnAcquireEffect(OnAcquireBuildingEffect effect) {
        onAcquireBuildingEffects.add(effect);
    }
    public void registerEndGameEffect(EndGameBuildingEffect effect) {
        endGameBuildingEffects.add(effect);
    }

    // getters
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

    // query methods — tutta la logica di conteggio vive qui
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

    public int calculateBuildersEndGamePoints() {
        return builders.stream()
                .mapToInt(BuilderCard::getPrestigePoints)
                .sum();
    }

    /**
     * 10 PP for every 2 Artists in your tribe.
     */
    public int calculateArtistEndGamePoints() {
        return (artists.size() / 2) * 10;
    }

    /**
     * PP equal to the number of Inventors multiplied by
     * the number of different Invention icons on the respective cards.
     */
    // TODO da controllare
    public int calculateInventorEndGamePoints() {
        return getInventorCount() * getDistinctInventionIcons();
    }

    /**
     * PP from Buildings: the printed Prestige Points on each card.
     * EndOfGameEffect bonuses are NOT calculated here — they are
     * registered in the Player and called separately by EndOfGamePhase.
     */
    // TODO da unire le varie liste buildings in una sola
    public int calculateBuildingPrintedPoints() {
        return buildings.stream()
                .mapToInt(BuildingCard::getEndGamePoints)
                .sum();
    }

    // capire meglio in base a come verrà usata
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
