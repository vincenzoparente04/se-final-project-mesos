package model.cards;

public class BuilderCard extends CharacterCard {
    private final int buildingDiscount;  // food discount for each building construction
    private final int endGamePoints;     // prestige points provided at the end of the game

    public int getBuildingDiscount()

    @Override
    public int calculateEndGamePoints() { return endGamePoints; }
}

