package model.cards.characterCards;

import model.enums.Era;
import model.player.Player;

public class BuilderCard extends CharacterCard {
    private final int builderDiscount;  // food discount for each building construction
    private final int prestigePoints;

    public BuilderCard(int id, Era era, int playerCount, int buildingDiscount, int pp, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
        this.builderDiscount = buildingDiscount;
        this.prestigePoints = pp;
    }

    public int getPrestigePoints() { return prestigePoints; }
    public int getBuilderDiscount(){ return builderDiscount; }

    /**
     * Registers this card by adding a builder to the player's tribe.
     *
     * @param player the player who drew this card
     */
    @Override
    public void registerToTribe(Player player) {
        player.getTribe().addBuilder(this);
    }
}