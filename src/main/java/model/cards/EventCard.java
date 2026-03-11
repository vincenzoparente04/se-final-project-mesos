package model.cards;

import model.enums.EventType;

abstract class EventCard extends TribeCard {
    // HUNT, SHAMANIC_RITUAL, CAVE_PAINTINGS, SUSTENANCE
    private final EventType eventType;

    // metodi
    public EventType getEventType()

    abstract void resolve(List<Player> players)
}