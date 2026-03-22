package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.cards.Card;
import model.enums.GamePhase;
import model.board.OfferTileAction.OfferTileAction
import model.enums.TotemLocation;
import model.player.Player;

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
        currentPlayer = getNextPlayerOnOfferTrack();

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

    @Override
    public void drawCard(int cardId) {
        ensureActiveTurn();

        Board board = model.getBoard();
        Card card = board.findCardById(cardId);

        // ha senso?
        if (card == null) {
            throw new IllegalArgumentException("Carta non trovata sul tabellone.");
        }

        // controlla che il player stia pescando dalla row giusta
        if (!currentAction.canDraw(card, board)) {
            throw new IllegalStateException("La tessera Offerta non ti permette di pescare questa carta (riga errata o limite raggiunto).");
        }

        // controlla che la carta non sia un evento o un building troppo costoso per il player
        if (!card.canBeAcquiredBy(currentPlayer, model)) {
            throw new IllegalStateException("Non hai i requisiti per prendere questa carta (Cibo insufficiente o è un Evento).");
        }

        // -- ESECUZIONE PESCA --
        // Rimuove la carta dal board
        board.removeCard(cardId);

        // La carta gestisce l'aggiunta alla tribù, il pagamento, effetti immediati
        // ###sistemare i metodi di pesca nelle carte
        card.acquiredBy(currentPlayer, model);

        // L'azione scala i suoi contatori interni
        currentAction.performDraw(card, board);

        model.notifyChange("card_drawn:" + cardId);

        // Dopo ogni pescata, verifichiamo se il turno è finito o deve essere forzatamente terminato
        checkActionCompletionOrAutoAdvance();
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

    // ###capire bene se serve davvero considerando che si è obbligati a pescare solo i personaggi
    private boolean hasAnyLegalMove() {
        Board board = model.getBoard();

        for (Card card : board.getAllCardsOnBoard()) {
            // Se l'azione gli permette di guardare a questa riga...
            if (currentAction.canDraw(card, board)) {
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
