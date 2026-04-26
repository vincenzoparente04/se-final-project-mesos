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

        if (currentAction == null) {
            throw new IllegalStateException("No action associated with the offer tile.");
        }

        currentAction.onEnterAction(currentPlayer, model);

        model.notifyChange("action_started:" + currentPlayer.getName());

        checkActionCompletionOrAutoAdvance();
    }

    /**
     * @implNote delegates all the logic to CardDrawer which uses visitor pattern to check if the card can be drawn and if yes how to manage the drawing
     * @param cardId
     */
    @Override
    public void drawCard(int cardId) {
        ensureActiveTurn();

        RowsManager rowsManager = model.getRowsManager();
        Card card = rowsManager.findCardById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found on board: " + cardId));

        if (!currentAction.canDraw(card, rowsManager)) {
            throw new IllegalStateException("The offer tile does not allow drawing this card (wrong row or draw limit reached).");
        }

        CardDrawer cardDrawer = new CardDrawer(currentPlayer, rowsManager, currentAction);
        cardDrawer.drawCard(card);

        model.notifyChange("card_drawn:" + cardId);

        // Dopo ogni pescata, verifichiamo se il turno è finito o deve essere forzatamente terminato
        checkActionCompletionOrAutoAdvance();
    }

    public void endTurn() {
        ensureActiveTurn();

        if (hasAnyForcedMove()) {
            throw new IllegalStateException("All mandatory draws must be completed before ending the turn.");
        }

        model.notifyChange("turn_ended:" + currentPlayer.getName());
        advanceActionTurn();
    }

    /**
     * checks if the action is completed (the player drawn all cards the tile action imposed to him)
     * or if there are isn't any legal move (no character card or acquirable building card left)
     * these are the only two cases when the turn advances automatically
     */
    private void checkActionCompletionOrAutoAdvance() {
        if (currentAction.isFinished() || !hasAnyLegalMove()) {
            advanceActionTurn();
        }
    }

    /**
     * @implNote checks if there are no character card or acquirable building card left using legalMoveChecker which uses visitor pattern
     * @return
     */
    private boolean hasAnyLegalMove() {
        RowsManager rowsManager = model.getRowsManager();
        MoveChecker moveChecker = new MoveChecker(currentPlayer, currentAction,  rowsManager);

        return moveChecker.checkLegalMoves(rowsManager.getAllCardsOnBoard());
    }

    /**
     * @implNote checks if there are no character card left using legalMoveChecker which uses visitor pattern
     * @return
     */
    private boolean hasAnyForcedMove() {
        RowsManager rowsManager = model.getRowsManager();
        MoveChecker moveChecker = new MoveChecker(currentPlayer, currentAction,  rowsManager);

        return moveChecker.checkForcedMoves(rowsManager.getAllTribeCardsOnBoard());
    }

    private void advanceActionTurn() {
        ensureActiveTurn();

        model.getBoard().returnTotemToTurnOrder(currentPlayer);

        currentPlayer = null;
        currentAction = null;

        // Ricomincia il ciclo per il prossimo giocatore
        startNextPlayerTurn();
    }

    private void ensureActiveTurn() {
        if (currentPlayer == null || currentAction == null) {
            throw new IllegalStateException("No active action turn.");
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
