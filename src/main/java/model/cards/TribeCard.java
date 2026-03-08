package model.cards;

// it's a group of cards which include character e event cards, wich are the types of cards in the deck
public abstract class TribeCard extends Card {
    // returns the effect to apply when a player recruits this card. Null if it has no effect.
    // (HunterCard is the only class which override it).
    // The Controller calls this method immediately when a player recruits the card, before applying the effect
    // there is no logic in the card, it only describes the effect, while the Controller is responsible for applying it to the game state
    public ImmediateEffect getImmediateEffect() {return null;};
}