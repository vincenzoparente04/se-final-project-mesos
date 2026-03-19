package model.player;

import model.cards.buildingCards.BuildingCard;
import model.cards.charachterCards.*;
import model.enums.CharacterType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Tribe {
    // 6 liste diverse per gestire i vari tipi di character card, più una lista per i building card
    private final List<ArtistCard> artists = new ArrayList<>();
    private final List<BuilderCard> builders = new ArrayList<>();
    private final List<GathererCard> gatherers = new ArrayList<>();
    private final List<HunterCard> hunters = new ArrayList<>();
    private final List<InventorCard> inventors = new ArrayList<>();
    private final List<ShamanCard> shamans = new ArrayList<>();

    private List<BuildingCard> buildings = new ArrayList<>();

    // metodi per aggiungere le carte alle varie liste
    public void addArtist(ArtistCard card) { artists.add(card); }
    public void addBuilder(BuilderCard card) { builders.add(card); }
    public void addGatherer(GathererCard card) { gatherers.add(card); }
    public void addHunter(HunterCard card) { hunters.add(card); }
    public void addInventor(InventorCard card) { inventors.add(card); }
    public void addShaman(ShamanCard card) { shamans.add(card); }

    public void addBuilding(BuildingCard card) { buildings.add(card); }


    // query methods — tutta la logica di conteggio vive qui
    public int getHunterCount()         { return hunters.size(); }
    public int getArtistCount()         { return artists.size(); }
    public int getBuilderCount()        { return builders.size(); }
    public int getInventorCount()       { return inventors.size(); }
    public int getGathererCount()       { return gatherers.size(); }
    public int getShamanCount()         { return shamans.size(); }

    public int getTotalCharacterCount() {
        return hunters.size() + builders.size() + shamans.size()
                + artists.size() + inventors.size() + gatherers.size();
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
        return (int) inventors.stream()
                .map(InventorCard::getInventionIcon)
                .distinct()
                .count();
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
        long distinctIcons = inventors.stream()
                .map(InventorCard::getInventionIcon)
                .distinct()
                .count();

        return inventors.size() * (int) distinctIcons;
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
                inventors.size(),
                shamans.size()
        ).min(Integer::compareTo).orElse(0);
    }
}
