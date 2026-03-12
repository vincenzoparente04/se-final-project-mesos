package model.cards;

import model.enums.EventType;
import model.player.Player;

import java.util.List;

abstract class EventCard extends TribeCard {
    // HUNT, SHAMANIC_RITUAL, CAVE_PAINTINGS, SUSTENANCE
    private final EventType eventType;

    // metodi
    abstract void resolve(List<Player> players);
}