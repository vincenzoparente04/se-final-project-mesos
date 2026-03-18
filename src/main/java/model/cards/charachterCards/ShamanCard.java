package model.cards.charachterCards;

public class ShamanCard extends CharacterCard {
    private final int starCount;  // number of stars

    public ShamanCard(int id, model.enums.Era era, int playerCount, int starCount) {
        super(id, era, playerCount);
        this.starCount = starCount;
    }

    public int getStarCount(){ return starCount; }

    @Override
    public void registerToTribe(model.player.Player player) {
        player.getTribe().addShaman(this);
    }
}