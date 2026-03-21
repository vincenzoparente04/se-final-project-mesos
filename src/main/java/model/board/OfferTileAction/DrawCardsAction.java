package model.board.OfferTileAction;

import model.GameModel;
import model.board.Board;
import model.cards.Card;
import model.player.Player;

public class DrawCardsAction {
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
    public boolean canDraw(Card card, Board board) {
        // La carta si trova nella riga superiore e non ho ancora raggiunto il limite
        if (board.getTopRow().containsCard(card.getId())) {
            return currentTopRowDraws < maxTopRowDraws;
        }

        // La carta si trova nella riga inferiore e non ho ancora raggiunto il limite
        if (board.getBottomRow().containsCard(card.getId())) {
            return currentBottomRowDraws < maxBottomRowDraws;
        }

        return false;
    }

    @Override
    public void performDraw(Card card, Board board) {
        // Aggiorna il contatore giusto in base a dove si trovava la carta
        if (board.getTopRow().containsCard(card.getId())) {
            currentTopRowDraws++;
        } else if (board.getBottomRow().containsCard(card.getId())) {
            currentBottomRowDraws++;
        }
    }

    @Override
    public boolean isFinished() {
        // L'azione si conlude quando i contatori hanno raggiunto il limite
        return (currentTopRowDraws >= maxTopRowDraws) &&
                (currentBottomRowDraws >= maxBottomRowDraws);
    }
}
