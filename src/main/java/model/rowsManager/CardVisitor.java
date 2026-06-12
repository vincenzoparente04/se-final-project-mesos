package model.rowsManager;

import model.cards.buildingCards.BuildingCard;
import model.cards.characterCards.ArtistCard;
import model.cards.characterCards.BuilderCard;
import model.cards.characterCards.CharacterCard;
import model.cards.characterCards.GathererCard;
import model.cards.characterCards.HunterCard;
import model.cards.characterCards.InventorCard;
import model.cards.characterCards.ShamanCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;

/**
 * Visitor interface for processing different types of cards polymorphically.
 *
 * Implements the Visitor design pattern to allow operations on different card types
 * without relying on type casting or {@code instanceof} checks. This is primarily used
 * by {@link EventResolver} to identify and categorize event cards.
 *
 * Beyond the coarse-grained categories (character, event, building), the interface
 * exposes a dedicated overload for each concrete character role. These fine-grained
 * methods default to the generic {@link #visit(CharacterCard)} handler, so visitors
 * that only care about the broad category can ignore them, while visitors that need
 * role-specific behavior (such as building presentation DTOs) can override them
 * selectively.
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

    /**
     * Visits a hunter card. Defaults to the generic character handler.
     *
     * @param card the hunter card to visit
     */
    default void visit(HunterCard card) { visit((CharacterCard) card); }

    /**
     * Visits a shaman card. Defaults to the generic character handler.
     *
     * @param card the shaman card to visit
     */
    default void visit(ShamanCard card) { visit((CharacterCard) card); }

    /**
     * Visits a builder card. Defaults to the generic character handler.
     *
     * @param card the builder card to visit
     */
    default void visit(BuilderCard card) { visit((CharacterCard) card); }

    /**
     * Visits an artist card. Defaults to the generic character handler.
     *
     * @param card the artist card to visit
     */
    default void visit(ArtistCard card) { visit((CharacterCard) card); }

    /**
     * Visits an inventor card. Defaults to the generic character handler.
     *
     * @param card the inventor card to visit
     */
    default void visit(InventorCard card) { visit((CharacterCard) card); }

    /**
     * Visits a gatherer card. Defaults to the generic character handler.
     *
     * @param card the gatherer card to visit
     */
    default void visit(GathererCard card) { visit((CharacterCard) card); }
}
