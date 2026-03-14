package model.player;

import model.cards.BuildingCard;
import model.cards.CharacterCard;
import model.enums.CharacterType;

import java.util.List;

public class Tribe {
    // flat list of all the cards in the tribe, listed in order of recruitment
    // (first recruited → first in the list)
    private List<CharacterCard> characters;
    private List<BuildingCard> buildings;

    // -- addition of cards --
    public void addCharacter(CharacterCard card)
    public void addBuilding(BuildingCard card)

    //getter
    public List<CharacterCard> getAllCharacters()
    public List<BuildingCard> getAllBuildings()

    // -- query for type --
    // used by EventResolver, CostCalculator, calculateEndGamePoints

    public int countByType(CharacterType type) {}

    // returns all the CharacterCard of the specified type, listed in order of recruitment
    // ###potrebbe non servire
    public List<CharacterCard> getByType(CharacterType type)


    // -- query for specific types --
    // sums all the stars of the ShamanCard in the tribe used by EventResolver during
    // Shamanic Ritual
    public int getTotalShamanStars(){}

    // sums the discounts of all the BuilderCard used by CostCalculator when a player wants
    // to take a Building
    public int getTotalBuilderDiscount()

    // sums the total prestige points given by builders at the end of the game
    public int getTotalBuilderPrestigePoints()

    // returns the number of DIFFERENT icons present among all the Inventor used by
    // calculateEndGamePoints of the Inventor
    public int getDifferentInventionIcons()

    // used by EventResolver during Sustenance
    public int countGatherers()

    // dice lo sconto durente l'evento dovuto ai raccoglitori -> rende inutile countGatherers
    // numRaccoglitori * 3
    public int getTotalGatherersDiscount(){}


    // -- query for set (used by some buildingCard) --
    // complete sets of CharacterCard of different colors give points at the end of the game
    public int countCompleteSets()

    // total number of Character card - used during Sustenance
    public int getTotalCharacterCount()


    // used by EventResolver and EndOfGameManager to find "active" buildings in that moment
    public List<BuildingCard> getBuildingsWithTrigger(BuildingEffectTrigger trigger)


    // -- query for the View --
    // groups cards by characterType, used by the view to display them in the correct position in the tribe
    public Map<CharacterType, List<CharacterCard>> getCharactersByCharactersType()
}
