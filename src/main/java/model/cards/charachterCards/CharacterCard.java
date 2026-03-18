package model.cards.charachterCards;

import model.cards.Drawable;
import model.board.CardVisitor;
import model.cards.TribeCard;
import model.enums.Era;
import model.player.Player;

public abstract class CharacterCard extends TribeCard{
    public CharacterCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }

    @Override
    public void draw(Player player){
        registerToTribe(player);
    }

    /**
     * @implNote Registers this character card inside the correct list
     *           of the player's tribe.
     * @param player
     */
    public abstract void registerToTribe(Player player);

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}