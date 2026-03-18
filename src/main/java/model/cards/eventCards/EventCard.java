package model.cards.eventCards;

import model.board.CardVisitor;
import model.cards.TribeCard;
import model.enums.EventType;
import model.player.Player;

import java.util.List;

public abstract class EventCard extends TribeCard {
    // HUNT, SHAMANIC_RITUAL, CAVE_PAINTINGS, SUSTENANCE
    private final EventType eventType;

    // metodi
    public abstract void resolve(List<Player> players);

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}