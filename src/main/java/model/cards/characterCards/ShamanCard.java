package model.cards.characterCards;

public class ShamanCard extends CharacterCard {
    private final int starCount;  // number of stars

    public ShamanCard(int id, model.enums.Era era, int playerCount, int starCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
        this.starCount = starCount;
    }

    public int getStarCount(){ return starCount; }

    /**
     * Registers this card by adding a shaman to the player's tribe.
     *
     * @param player the player who drew this card
     */
    @Override
    public void registerToTribe(model.player.Player player) {
        player.getTribe().addShaman(this);
    }
}