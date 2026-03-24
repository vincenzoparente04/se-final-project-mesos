package model.cards.charachterCards;

public class ShamanCard extends CharacterCard {
    private final int starCount;  // number of stars

    public ShamanCard(int id, model.enums.Era era, int playerCount, int starCount) {
        super(id, era, playerCount);
        this.starCount = starCount;
    }

    public int getStarCount(){ return starCount; }

    /**
     * @implNote Add a shaman to its list in the tribe of the player who picked this card.
     * @param player
     */
    @Override
    public void registerToTribe(model.player.Player player) {
        player.getTribe().addShaman(this);
    }
}