package model.phaseHandlers;

import model.board.OfferTileAction.OfferTileAction;
import model.cards.Card;
import model.cards.TribeCard;
import model.cards.buildingCards.BuildingCard;
import model.cards.characterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.player.Player;
import model.rowsManager.CardVisitor;
import model.rowsManager.RowsManager;

import java.util.List;

/**
 * Evaluates the legality of potential card draws for a specific player,
 * determining if any valid or forced moves remain available on the board.
 *<p>This class implements the <em>Visitor</em> pattern to apply type-specific
 * validation rules dynamically. It evaluates cards based on the player's current
 * {@link model.board.OfferTileAction.OfferTileAction} and their available resources
 * (e.g., food).
 * </p>
 * The validation follows these core rules:
 * <ul>
 * <li><b>Character Cards:</b> Valid if permitted by the current tile action.</li>
 * <li><b>Building Cards:</b> Valid only if the player possesses sufficient food
 * to cover the card's cost and is permitred by the tile action.</li>
 * <li><b>Event Cards:</b> Strictly invalid, as they cannot be drawn by players.</li>
 * </ul>
 * <p>For performance optimization, the checking mechanisms ({@code checkLegalMoves}
 * and {@code checkForcedMoves}) utilize short-circuit evaluation, halting iteration
 * as soon as the first legal move is identified.
 * </p>
 * @see model.rowsManager.CardVisitor
 * @see ActionPhase
 */
public class MoveChecker implements CardVisitor {
    private boolean result;
    private Player player;
    private OfferTileAction currentAction;
    private RowsManager rowsManager;

    public MoveChecker(Player currentPlayer, OfferTileAction currentAction, RowsManager rowsManager) {
        this.result = false;
        this.player = currentPlayer;
        this.currentAction = currentAction;
        this.rowsManager = rowsManager;
    }

    /**
     * Scans the provided list of Tribe Cards (Characters and Events) to determine
     * if at least one mandatory or permissible move exists.
     * <p>
     * <b>Performance Note:</b> Utilizes short-circuit evaluation. The iteration
     * halts immediately upon identifying the first valid card.
     * </p>
     * @param allTribeCardsOnBoard a list of all currently visible tribe cards on board.
     * @return {@code true} if at least one valid draw is found, {@code false} otherwise.
     */
    public boolean checkForcedMoves(List<TribeCard> allTribeCardsOnBoard) {
        for (TribeCard card : allTribeCardsOnBoard) {
            if (result) {return true;} //if result is already true, it means that the player has at least one legal move, so we can stop checking
            if (currentAction.canDraw(card, rowsManager)) {
                card.accept(this);
            }
        }
        return result;
    }

    /**
     * Scans all board cards (including Buildings) to determine if the player
     * has any legal moves available, factoring in both tile actions and resource limits.
     * <p>
     * <b>Performance Note:</b> Utilizes short-circuit evaluation.
     * </p>
     * @param allCardsOnBoard a list of all cards currently visible on the board.
     * @return {@code true} if at least one legal move is found, {@code false} otherwise.
     */
    public boolean checkLegalMoves(List<Card> allCardsOnBoard) {
        for (Card card : allCardsOnBoard) {
            if (result) {return true;} //if result is already true, it means that the player has at least one legal move, so we can stop checking
            if (currentAction.canDraw(card, rowsManager)) {
                card.accept(this);
            }
        }
        return result;
    }

    /**
     * Flags a Character Card as universally drawable.
     * @param card the character card being evaluated.
     */
    @Override
    public void visit(CharacterCard card) {
        result = true;
    }

    @Override
    public void visit(EventCard card) {
        //it's not a legal move
    }

    @Override
    public void visit(SustenanceEventCard card) {
        //it's not a legal move
    }

    /**
     * Flags a Building Card as drawable if the player has enough food to pay for it, considereing the possible discount.
     * @param card the building card being evaluated.
     */
    @Override
    public void visit(BuildingCard card) {
        if (player.getFood() >= card.getDiscountedCost(player)) {
            result = true;
        }
    }
}
