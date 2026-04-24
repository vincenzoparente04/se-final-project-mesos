package model.board.OfferTileAction;

import model.GameModel;
import model.cards.Card;
import model.player.Player;
import model.rowsManager.RowsManager;

public class DrawCardsAction implements OfferTileAction {
    // --- DEFINIZIONE (Valori base immutabili della tessera) ---
    private final int maxTopRowDraws;
    private final int maxBottomRowDraws;

    // --- STATO DELL'ESECUZIONE (Mutabile durante il turno) ---
    private int currentTopRowDraws;
    private int currentBottomRowDraws;

    public DrawCardsAction(int maxTopRowDraws, int maxBottomRowDraws) {
        this.maxTopRowDraws = maxTopRowDraws;
        this.maxBottomRowDraws = maxBottomRowDraws;
    }

    @Override
    public void onEnterAction(Player player, GameModel model) {
        // Ogni volta che un giocatore entra in questa azione,
        // i contatori ripartono da zero.
        this.currentTopRowDraws = 0;
        this.currentBottomRowDraws = 0;
    }

    @Override
    public boolean canDraw(Card card, RowsManager rowsManager) {
        // La carta si trova nella riga superiore e non ho ancora raggiunto il limite
        if (rowsManager.topRowContainsCard(card.getId())) {
            return currentTopRowDraws < maxTopRowDraws;
        }

        // La carta si trova nella riga inferiore e non ho ancora raggiunto il limite
        if (rowsManager.bottomRowContainsCard(card.getId())) {
            return currentBottomRowDraws < maxBottomRowDraws;
        }

        return false;
    }

    @Override
    public void performDraw(Card card, RowsManager rowsManager) {
        // Aggiorna il contatore giusto in base a dove si trovava la carta
        if (rowsManager.topRowContainsCard(card.getId())) {
            currentTopRowDraws++;
        } else if (rowsManager.bottomRowContainsCard(card.getId())) {
            currentBottomRowDraws++;
        }
    }

    @Override
    public boolean isFinished() {
        if ((currentTopRowDraws >= maxTopRowDraws) && (currentBottomRowDraws >= maxBottomRowDraws)) {
            onEnterAction(null, null); // Resetta i contatori per il prossimo turno
            return true;
        }
        return false;
    }

    public int getMaxTopRowDraws()        { return maxTopRowDraws; }
    public int getMaxBottomRowDraws()     { return maxBottomRowDraws; }
    public int getCurrentTopRowDraws()    { return currentTopRowDraws; }
    public int getCurrentBottomRowDraws() { return currentBottomRowDraws; }

    @Override
    public void accept(OfferTileActionVisitor visitor) { visitor.visitDrawCards(this); }
}
