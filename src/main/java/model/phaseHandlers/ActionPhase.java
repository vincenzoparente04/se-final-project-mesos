package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.cards.Card;
import model.enums.GamePhase;
import model.board.OfferTileAction.OfferTileAction;
import model.player.Player;
import model.rowsManager.RowsManager;

public class ActionPhase extends GamePhaseHandler {

    private Player currentPlayer;
    private OfferTileAction currentAction;

    public ActionPhase(GameModel model) {
        super(model);
    }

    @Override
    public void onEnter() {
        startNextPlayerTurn();
    }

    private void startNextPlayerTurn() {
        Board board = model.getBoard();

        // Trova il prossimo giocatore da sinistra a destra sul tracciato offerte
        currentPlayer = board.getNextPlayerOnOfferTrack();

        // Se non ci sono più giocatori, la fase Action è finita
        if (currentPlayer == null) {
            currentAction = null;
            model.setPhase(new PreEndOfRoundPhase(model));
            return;
        }

        currentAction = board.getOfferTrack()
                .getOccupiedTileByPlayer(currentPlayer)
                .getAction();

        // ###questo controllo è sensato?
        if (currentAction == null) {
            throw new IllegalStateException("Nessuna azione associata alla tessera Offerta.");
        }

        model.notifyChange("action_started:" + currentPlayer.getName());

        // Inizializza l'azione (es. la tessera "A" darà subito i 3 Cibo qui)
        currentAction.onEnterAction(currentPlayer, model);

        // controlla che effettivamente un player abbia completato l'azione
        checkActionCompletionOrAutoAdvance();
    }

    /**
     * @implNote delegates all the logic to CardDrawer which uses visitor pattern to check if the card can be drawn and if yes how to manage the drawing
     * @param cardId
     */
    @Override
    public void drawCard(int cardId) {
        CardDrawer cardDrawer = new CardDrawer(currentPlayer, model.getRowsManager(), currentAction);

        // Prima di ogni pescata, verifichiamo se il turno è finito o deve essere forzatamente terminato
        checkActionCompletionOrAutoAdvance();
        ensureActiveTurn();

        RowsManager rowsManager = model.getRowsManager();
        Card card = rowsManager.findCardById(cardId);

        // ha senso?
        if (card == null) {
            throw new IllegalArgumentException("Carta non trovata sul tabellone.");
        }

        // controlla che il player stia pescando dalla row giusta
        if (!currentAction.canDraw(card, rowsManager)) {
            throw new IllegalStateException("La tessera Offerta non ti permette di pescare questa carta (riga errata o limite raggiunto)."); // serve l'exception??
        }

        cardDrawer.drawCard(card);

        model.notifyChange("card_drawn:" + cardId);
    }

    /**
     * Controlla se l'azione è tecnicamente finita (contatori a 0)
     * OPPURE se il giocatore è in una situazione di "stallo" (es. restano solo Eventi o Edifici inarrivabili).
     * In Mesos, non c'è il pulsante "Passa", il gioco avanza se non hai mosse legali.
     */
    private void checkActionCompletionOrAutoAdvance() {
        if (currentAction.isFinished() || !hasAnyLegalMove()) {
            advanceActionTurn();
        }
    }

    private boolean hasAnyLegalMove() {
        model.rowsManager.RowsManager rows = model.getRowsManager();

        for (Card card : rows.getAllCardsOnBoard()) {
            // Se l'azione gli permette di guardare a questa riga...
            if (currentAction.canDraw(card, rows)) {
                // ... e la carta può essere fisicamente presa ...
                if (card.canBeAcquiredBy(currentPlayer, model)) {
                    return true; // Ha ancora qualcosa che PUÒ e DEVE pescare
                }
            }
        }
        return false;
    }

    private void advanceActionTurn() {
        ensureActiveTurn();

        // Riporta il totem sulla prima tile
        // La logica del pagamento di 1 cibo per l'ultimo posto va gestita dentro questo metodo!
        model.getBoard()
                .getTurnOrderTile()
                .returnTotemAndResolveEffects(currentPlayer);

        currentPlayer = null;
        currentAction = null;

        // Ricomincia il ciclo per il prossimo giocatore
        startNextPlayerTurn();
    }

    private void ensureActiveTurn() {
        if (currentPlayer == null || currentAction == null) {
            throw new IllegalStateException("Nessun turno di azione attivo.");
        }
    }

    @Override
    public GamePhase getPhase() {
        return GamePhase.ACTION;
    }

    @Override
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
}
