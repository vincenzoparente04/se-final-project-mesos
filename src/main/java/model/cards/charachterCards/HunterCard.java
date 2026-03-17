package model.cards.charachterCards;

import model.effects.ImmediateEffect;

public class HunterCard extends CharacterCard {
    // some hunter cards have a trigger icon, which means that when a player recruits them, they immediately trigger an effect that gives them food based
    // on the number of Hunter cards in their tribe (including the one they just recruited). This is represented by the hasTriggerIcon boolean field, and
    // the getImmediateEffect method returns the effect to apply when the card is recruited if hasTriggerIcon is true, or null if it is false.
    private final boolean hasTriggerIcon;

    // if hasTriggerIcon is true, the immediate effect is to give the player food equal to the number of Hunter cards in their tribe (including the one they just recruited), otherwise there is no immediate effect.
    @Override
    public ImmediateEffect getImmediateEffect(){};
}
