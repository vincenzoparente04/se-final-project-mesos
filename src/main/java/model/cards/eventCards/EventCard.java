package model.cards.eventCards;

import model.GameModel;
import model.board.CardVisitor;
import model.cards.TribeCard;
import model.enums.Era;
import model.enums.EventType;
import model.player.Player;

import java.util.List;

public abstract class EventCard extends TribeCard {
    // HUNT, SHAMANIC_RITUAL, CAVE_PAINTINGS, SUSTENANCE
    // ###possiamo toglierlo?
    private final EventType eventType;

    public EventCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }

    @Override
    public boolean canBeAcquiredBy(Player player, GameModel model) {
        return false;
    }

    // questo metodo non dovrebbe mai essere chiamato -> di può togliere?
    @Override
    public void acquiredBy(Player player, GameModel model) {
        throw new IllegalStateException("Le carte Evento non possono mai essere acquisite!");
    }
    }

    // metodi
    public abstract void resolve(List<Player> players);

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}