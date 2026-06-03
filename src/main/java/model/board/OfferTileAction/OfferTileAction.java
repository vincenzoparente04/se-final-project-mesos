package model.board.OfferTileAction;

import model.GameModel;
import model.cards.Card;
import model.player.Player;
import model.rowsManager.RowsManager;

public interface OfferTileAction {
    void onEnterAction(Player player, GameModel model);

    boolean canDraw(Card card, RowsManager rowsManager);

    void performDraw(Card card, RowsManager rowsManager);

    boolean isFinished();

    void accept(OfferTileActionVisitor visitor);
}
