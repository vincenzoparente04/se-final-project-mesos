package model.phaseHandlers;

import model.board.OfferTileAction.OfferTileAction;
import model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects.OnAcquireBuildingEffect;
import model.cards.Card;
import model.cards.buildingCards.BuildingCard;
import model.cards.characterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.player.Player;
import model.rowsManager.CardVisitor;
import model.rowsManager.RowsManager;

/**
 * Executes the acquisition of a card from the board, managing the transfer
 * of the card to the player's tribe and the application of associated costs and effects.
 * * <p>Acting as a state-mutating <em>Visitor</em>, this class leverages double-dispatch
 * to process different card types safely without explicit casting:
 * <ul>
 * <li><b>Character Cards:</b> Removed from the board, registered to the player's tribe,
 * and immediately trigger any active {@link model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects.OnAcquireBuildingEffect}.</li>
 * <li><b>Building Cards:</b> Validates the player's food reserves, deducts the
 * appropriately discounted cost, removes the card from the board, and registers it.</li>
 * <li><b>Event Cards:</b> Throws an {@link IllegalStateException} since event cards
 * are resolved automatically by the game engine and cannot be manually drawn.</li>
 * </ul>
 * </p>
 * <p>This handler gracefully adapts to contexts where a standard {@code OfferTileAction}
 * is absent (e.g., during the end-of-round bonus draft), applying the base acquisition
 * logic without tile-specific modifiers.
 * </p>
 * @see model.rowsManager.CardVisitor
 * @see ActionPhase
 */
public class CardDrawer implements CardVisitor {
    private Player player;
    private RowsManager rowsManager;
    private OfferTileAction currentAction;

    CardDrawer(Player player, RowsManager rowsManager, OfferTileAction currentAction) {
        this.player = player;
        this.rowsManager = rowsManager;
        this.currentAction = currentAction;
    }

    /**
     * Initiates the double-dispatch mechanism to execute the type-specific
     * drawing logic for the provided card.
     * * @param card the {@link Card} instance to be acquired by the player.
     */
    public void drawCard(Card card) {
        card.accept(this);
    }

    /**
     * Executes the acquisition pipeline for a Character Card.
     * <p>
     * The process guarantees the following sequence:
     * <p>1. Application of the current tile's draw action (if present).
     * <p>2. Removal of the card from the board.
     * <p>3. Registration of the card into the player's tribe.
     * <p>4. Resolution of any active {@link OnAcquireBuildingEffect} triggered by this addition.
     * </p>
     * @param card the character card being acquired.
     */
    @Override
    public void visit(CharacterCard card) {
        if (currentAction != null) { // null when called by PreEndOfRoundPhase (no tile action involved)
            currentAction.performDraw(card, rowsManager);
        }

        rowsManager.removeCard(card.getId());

        card.registerToTribe(player);

        for (OnAcquireBuildingEffect effect : player.getTribe().getOnAcquireBuildingEffects()) {
            effect.applyEffect(player);
        }
    }

    /**
     * Handles the transactional acquisition of a Building Card.
     * <p>
     * Acts by enforcing food availability pre-conditions.
     * If the player possesses sufficient food (after applying any discounts),
     * the cost is deducted, the card is removed from the board and registered to the player's tribe.
     * </p>
     * @param card the building card being acquired.
     * @throws IllegalStateException if the player's food is strictly less than
     * the card's discounted cost.
     */
    @Override
    public void visit(BuildingCard card) {
        if (player.getFood() < card.getDiscountedCost(player)) {
            throw new IllegalStateException("Insufficient food to acquire building card.");
        } else {
            if (currentAction != null) { // null when called by PreEndOfRoundPhase (no tile action involved)
                currentAction.performDraw(card, rowsManager);
            }

            rowsManager.removeCard(card.getId());

            player.removeFood(card.getDiscountedCost(player));
            card.registerToTribe(player);
        }
    }

    /**
     * Rejects illegal acquisition attempts for Event Cards.
     * @param card the event card targeted for drawing.
     * @throws IllegalStateException always, as event cards can never be drawn.
     */
    @Override
    public void visit(EventCard card) {
        throw new IllegalStateException("Event cards cannot be drawn.");
    }

    /**
     * Rejects illegal acquisition attempts for Sustenance Event Cards.
     * @param card the event card targeted for drawing.
     * @throws IllegalStateException always, as event cards can never be drawn.
     */
    @Override
    public void visit(SustenanceEventCard card) {
        throw new IllegalStateException("Event cards cannot be drawn.");
    }
}
