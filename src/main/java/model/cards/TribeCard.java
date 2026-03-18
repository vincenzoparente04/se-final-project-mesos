package model.cards;

import model.effects.ImmediateEffect;
import model.enums.Era;

// it's a group of cards which include character e event cards, wich are the types of cards in the deck
public abstract class TribeCard extends Card {
    public TribeCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }
}