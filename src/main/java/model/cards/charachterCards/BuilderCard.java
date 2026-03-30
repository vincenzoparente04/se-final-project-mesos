package model.cards.charachterCards;

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
     * @implNote Add a builder to its list in the tribe of the player who picked this card.
     * @param player
     */
    @Override
    public void registerToTribe(Player player) {
        player.getTribe().addBuilder(this);
    }
}

