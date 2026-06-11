package model.board.OfferTileAction;

import model.GameModel;
import model.cards.Card;
import model.player.Player;
import model.rowsManager.RowsManager;

/**
 * Defines the contract for an action that can be performed on an offer tile.
 * 
 * This interface abstracts different types of offer tile interactions using the
 * <u><b>Strategy pattern</b></u>, allowing each action to define its own entry logic, card drawing
 * constraints, and completion conditions. It also implements the Visitor pattern
 * via the {@link #accept(OfferTileActionVisitor) accept} method for extensible
 * processing.
 * 
 * @see OfferTileActionVisitor
 * @see Card
 */
public interface OfferTileAction {

    /**
     * Called when a player enters this action on an offer tile.
     * <p>
     * This method allows the action to perform initialization logic, such as
     * displaying available cards or resetting state.
     *
     * @param player the player entering this action
     * @param model  the current game model
     */
    void onEnterAction(Player player, GameModel model);

    /**
     * Determines whether a card can be drawn by the current player.
     *
     * This method enforces action-specific constraints on which cards are
     * drawable given the current state of the rows' manager.
     *
     * @param card the card to evaluate for drawing
     * @param rowsManager the rows manager containing the card state
     * @return true if the card can be drawn, false otherwise
     */
    boolean canDraw(Card card, RowsManager rowsManager);

    /**
     * This method verify that the card can be drawn via {@link #canDraw(Card, RowsManager)}.
     * If so, it updates currentTopRowDraws and currentBottomRowDraws {@link DrawCardsAction} attributes accordingly.
     *
     * @param card        the card being drawn
     * @param rowsManager the rows manager to update with the draw
     */
    void performDraw(Card card, RowsManager rowsManager);

    /**
     * Determines whether this action has been completed.
     *
     * @return true if the action is finished and the offer tile interaction should end,
     * false otherwise
     */
    boolean isFinished();

    /**
     * Accepts a visitor for processing this action.
     * <p>
     * This method implements the Visitor pattern, allowing external logic to perform
     * type-specific operations on different action implementations.
     *
     * @param visitor the visitor that will process this action
     */
    void accept(OfferTileActionVisitor visitor);
}

