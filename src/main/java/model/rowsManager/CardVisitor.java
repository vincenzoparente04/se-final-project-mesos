package model.rowsManager;

import model.cards.buildingCards.BuildingCard;
import model.cards.characterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;

/**
 * Visitor interface for processing different types of cards polymorphically.
 * 
 * Implements the Visitor design pattern to allow operations on different card types
 * without relying on type casting or {@code instanceof} checks. This is primarily used
 * by {@link EventResolver} to identify and categorize event cards.
 * 
 * @see EventResolver
 */
public interface CardVisitor {

    /**
     * Visits a character card.
     *
     * @param card the character card to visit
     */
    void visit(CharacterCard card);

    /**
     * Visits a standard event card.
     *
     * @param card the event card to visit
     */
    void visit(EventCard card);

    /**
     * Visits a sustenance event card.
     *
     * @param card the sustenance event card to visit
     */
    void visit(SustenanceEventCard card);

    /**
     * Visits a building card.
     *
     * @param card the building card to visit
     */
    void visit(BuildingCard card);
}
