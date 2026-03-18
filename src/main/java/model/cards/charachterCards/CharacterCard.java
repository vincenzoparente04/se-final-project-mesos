package model.cards.charachterCards;

import model.board.CardVisitor;
import model.cards.TribeCard;
import model.enums.CharacterType;

public abstract class CharacterCard extends TribeCard {



    public CharacterType getCharacterType()

    // returns the number of prestige points provided by this card at the end of the game. (not 0 only for builders)
    // the Controller calls this method at the end of the game to calculate the player's score.
    public int calculateEndGamePoints(){return 0;}


    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}