package model.cards.eventCards;

import model.cards.TribeCard;
import model.enums.Era;
import model.enums.EventType;
import model.player.Player;

import java.util.List;

public abstract class EventCard extends TribeCard implements Drawable {
    // HUNT, SHAMANIC_RITUAL, CAVE_PAINTINGS, SUSTENANCE
    private final EventType eventType;

    public EventCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }

    // metodi
    abstract void resolve(List<Player> players);
}