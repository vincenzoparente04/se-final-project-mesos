package model.board.OfferTileAction;

import model.GameModel;
import model.cards.Card;
import model.player.Player;
import model.rowsManager.RowsManager;

/**
 * Implements the "Take Food" strategy for offer tile actions.
 * 
 * This action immediately grants a specified amount of food to a player when they
 * land on an offer tile with this action. The action is completed upon entry, as
 * no additional interaction (such as card drawing) is required.
 * 
 * @see OfferTileAction
 */
public class TakeFoodAction implements OfferTileAction {
    private final int foodAmount;
    private boolean finished;

    /**
     * Constructs a TakeFoodAction with a specified food amount.
     *
     * @param foodAmount the amount of food to grant to the player
     */
    public TakeFoodAction(int foodAmount) {
        this.foodAmount = foodAmount;
    }

    /**
     * Executes the food distribution when a player enters this offer tile action.
     * 
     * Immediately adds the specified amount of food to the player and marks
     * this action as finished, since no further interaction is required.
     *
     * @param player the player landing on this offer tile
     * @param model the current game model (not used for this action)
     */
    @Override
    public void onEnterAction(Player player, GameModel model) {
        // ...existing code...
        this.finished = true;
    }

    /**
     * Determines whether a card can be drawn in this action.
     * 
     * This action does not support card drawing; it only distributes food.
     *
     * @param card the card to evaluate (not used)
     * @param rowsManager the rows manager (not used)
     * @return always false as card drawing is not permitted in this action
     */
    @Override
    public boolean canDraw(Card card,  RowsManager rowsManager) {
        return false;
    }

    /**
     * Executes a card draw operation.
     * 
     * This action does not support card drawing, so this method performs no operation.
     *
     * @param card the card to draw (ignored)
     * @param rowsManager the rows manager to update (ignored)
     */
    @Override
    public void performDraw(Card card, RowsManager rowsManager) {
        // This action does not allow drawing cards.
    }

    /**
     * Determines whether this action has been completed.
     * 
     * Since food is distributed immediately upon entry, this action is always
     * finished after {@link #onEnterAction(Player, GameModel)} is called.
     *
     * @return true as this action completes immediately upon entry
     */
    @Override
    public boolean isFinished() {
        return finished;
    }

    /**
     * Accepts a visitor for processing this action.
     * 
     * Implements the Visitor pattern by dispatching to the visitor's
     * {@link OfferTileActionVisitor#visitTakeFood(TakeFoodAction) visitTakeFood} method.
     *
     * @param visitor the visitor that will process this action
     */
    @Override
    public void accept(OfferTileActionVisitor visitor) { 
        visitor.visitTakeFood(this); 
    }
}
