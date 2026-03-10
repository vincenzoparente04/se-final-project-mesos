package model.cards;

import model.enums.CharacterType;

// adds the character type to the card, which is used for the end game scoring.
// the classification of card type is kept separated between character, event and building
public abstract class CharacterCard extends TribeCard {
    private final CharacterType characterType; //GATHERER, BUILDER, INVENTOR...


    public CharacterType getCharacterType()

    // returns the number of prestige points provided by this card at the end of the game. (not 0 only for builders)
    // the Controller calls this method at the end of the game to calculate the player's score.
    public int calculateEndGamePoints(){return 0;}
}