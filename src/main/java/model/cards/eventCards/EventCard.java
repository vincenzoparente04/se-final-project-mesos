package model.cards.eventCards;

import model.board.CardVisitor;
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
    public abstract void resolve(List<Player> players);

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}