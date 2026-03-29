package model.rowsManager;

import model.cards.charachterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;

public interface CardVisitor {
    void visit(CharacterCard card);
    void visit(EventCard card);
    void visit(SustenanceEventCard card);
}
