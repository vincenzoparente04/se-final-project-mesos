package model.cards.charachterCards;

import model.enums.Era;
import model.player.Player;

public class BuilderCard extends CharacterCard {
    private final int builderDiscount;  // food discount for each building construction
    private final int prestigePoints;

    public BuilderCard(int id, Era era, int playerCount, int buildingDiscount, int pp) {
        super(id, era, playerCount);
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

