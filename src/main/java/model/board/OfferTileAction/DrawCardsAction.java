package model.board.OfferTileAction;

import model.GameModel;
import model.cards.Card;
import model.player.Player;
import model.rowsManager.RowsManager;

/**
 * Corresponds to the "Draw Cards" action on offer tiles. When a player steps on an offer tile with this action,
 * they can draw a specified number of cards from the top and bottom rows. The action is finished when the player
 * has drawn the maximum allowed cards from both rows.
 */
public class DrawCardsAction implements OfferTileAction {
    // --- Definied by rules, indicate the allowed draw from each row ---
    private final int maxTopRowDraws;
    private final int maxBottomRowDraws;

    // --- These two variables indicate the state of the action ---
    private int currentTopRowDraws;
    private int currentBottomRowDraws;

    public DrawCardsAction(int maxTopRowDraws, int maxBottomRowDraws) {
        this.maxTopRowDraws = maxTopRowDraws;
        this.maxBottomRowDraws = maxBottomRowDraws;
    }

    @Override
    public void onEnterAction(Player player, GameModel model) {
        // When the action starts, we reset the counters to 0
        this.currentTopRowDraws = 0;
        this.currentBottomRowDraws = 0;
    }

    /**
     * Identifies if a player can still draw from top or bottom rows.
     *
     * @param card to be drawn
     * @return a boolean indicating if the card can be drawn.
     */
    @Override
    public boolean canDraw(Card card, RowsManager rowsManager) {
        if (rowsManager.topRowContainsCard(card.getId())) {
            return currentTopRowDraws < maxTopRowDraws;
        }

        if (rowsManager.bottomRowContainsCard(card.getId())) {
            return currentBottomRowDraws < maxBottomRowDraws;
        }

        return false;
    }


    /**
     * Increments the variables which keep track of how much cards have
     * been already drawn.
     *
     * @param card To be drawn.
     */
    @Override
    public void performDraw(Card card, RowsManager rowsManager) {
        if (rowsManager.topRowContainsCard(card.getId())) {
            currentTopRowDraws++;
        } else if (rowsManager.bottomRowContainsCard(card.getId())) {
            currentBottomRowDraws++;
        }
    }

    /**
     * Checks if the action is finished, when the player has drawn the maximum allowed cards from both rows.
     *
     * @return A boolean indicating if the action is finished.
     */
    @Override
    public boolean isFinished() {
        return (currentTopRowDraws >= maxTopRowDraws) &&
                (currentBottomRowDraws >= maxBottomRowDraws);
    }

    public int getMaxTopRowDraws()        { return maxTopRowDraws; }
    public int getMaxBottomRowDraws()     { return maxBottomRowDraws; }
    public int getCurrentTopRowDraws()    { return currentTopRowDraws; }
    public int getCurrentBottomRowDraws() { return currentBottomRowDraws; }

    @Override
    public void accept(OfferTileActionVisitor visitor) { visitor.visitDrawCards(this); }
}
