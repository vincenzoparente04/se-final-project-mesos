package model.cards;

import model.enums.CardType;
import model.enums.Era;

public abstract class Card {
    private final int id;
    private final Era era;           // ERA_I, ERA_II, ERA_III
    private final int playerCount;   // minimum number of players required to have this card in the game (some cards are only used in games with 3, 4 or 5 players)
    private final CardType cardType;  // CHARACTER, EVENT, BUILDING
    // ### POTENZIALMENTE ELIMINABILE L'ATTRIBUTO CARD TYPE ###

    public abstract CardType getCardType()
    public int getId()
    public Era getEra()
    public int getMinPlayerCount()

}
