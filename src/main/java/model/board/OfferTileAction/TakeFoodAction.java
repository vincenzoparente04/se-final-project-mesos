package model.board.OfferTileAction;

import model.GameModel;
import model.board.Board;
import model.cards.Card;
import model.player.Player;
import model.rowsManager.RowsManager;

public class TakeFoodAction implements OfferTileAction {
    private final int foodAmount;
    private boolean finished; // Stato dell'esecuzione

    public TakeFoodAction(int foodAmount) {
        this.foodAmount = foodAmount; // Nel caso di Mesos sarà 3
    }

    @Override
    public void onEnterAction(Player player, GameModel model) {
        // Dà immediatamente il cibo al giocatore
        player.addFood(foodAmount);

        // Imposta l'azione come finita istantaneamente
        this.finished = true;
    }

    @Override
    public boolean canDraw(Card card,  RowsManager rowsManager) {
        return false; // Questa tessera non fa MAI pescare carte
    }

    @Override
    public void performDraw(Card card, RowsManager rowsManager) {
        // cancellare tanto non dovrebbe mai essere chiamato
        throw new UnsupportedOperationException("Questa azione non prevede pescate.");
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}
