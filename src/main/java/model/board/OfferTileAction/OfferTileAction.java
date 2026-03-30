package model.board.OfferTileAction;

import model.GameModel;
import model.cards.Card;
import model.player.Player;
import model.rowsManager.RowsManager;

public interface OfferTileAction {
    // Eseguito all'inizio. Utile per la tessera che da solo Cibo.
    void onEnterAction(Player player, GameModel model);

    // Controlla solo la competenza della tessera (Es. è nella Top Row? Ho ancora pescate Top Row?)
    boolean canDraw(Card card, RowsManager rowsManager);

    // Aggiorna i contatori interni dell'azione
    void performDraw(Card card, RowsManager rowsManager);

    // Ritorna true se tutti i contatori sono a 0
    boolean isFinished();
}
