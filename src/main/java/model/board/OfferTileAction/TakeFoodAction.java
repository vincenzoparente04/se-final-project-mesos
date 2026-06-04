package model.board.OfferTileAction;

import model.GameModel;
import model.cards.Card;
import model.player.Player;
import model.rowsManager.RowsManager;

/**
 * Corresponds to the "Take Food" action on offer tiles. When a player steps on an offer tile with this action,
 * they immediately receive a specified amount of food.
 */
public class TakeFoodAction implements OfferTileAction {
    private final int foodAmount;
    private boolean finished;

    public TakeFoodAction(int foodAmount) {
        this.foodAmount = foodAmount;
    }

    @Override
    public void onEnterAction(Player player, GameModel model) {
        // Immediately gives food to the player
        player.addFood(foodAmount);

        // In this case the action is already finished because it only gives food
        this.finished = true;
    }

    @Override
    public boolean canDraw(Card card,  RowsManager rowsManager) {
        return false;
    }

    @Override
    public void performDraw(Card card, RowsManager rowsManager) {
        throw new UnsupportedOperationException("TakeFoodAction does not support drawing cards");
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void accept(OfferTileActionVisitor visitor) { visitor.visitTakeFood(this); }
}
