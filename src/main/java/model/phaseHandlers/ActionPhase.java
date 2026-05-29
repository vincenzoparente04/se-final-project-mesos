package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.cards.Card;
import model.enums.GamePhase;
import model.board.OfferTileAction.OfferTileAction;
import model.player.Player;
import model.rowsManager.RowsManager;
import shared.command.gameCommand.DrawCardCommand;
import shared.command.gameCommand.EndTurnCommand;

/**
 * Handles the Action Phase of a game round, coordinating turn execution
 * for each player according to their position on the offer track.
 *
 * <p>This handler is responsible for the following lifecycle:
 * <ol>
 *   <li>Fetching players from the offer track in left-to-right order.</li>
 *   <li>Resolving the {@link OfferTileAction} associated with each player's tile.</li>
 *   <li>Validating and delegating card draw operations via {@link DrawCardCommand}.</li>
 *   <li>Advancing the turn automatically when an action is complete or no legal
 *       moves remain, or manually when triggered by an {@link EndTurnCommand}.</li>
 *   <li>Skipping disconnected players and correctly returning their totems
 *       to the turn order to preserve future round sequencing.</li>
 * </ol>
 *
 * <p>Command dispatch follows the <em>Visitor</em> pattern: incoming commands
 * (e.g., {@code DrawCardCommand}, {@code EndTurnCommand}) are routed to the
 * appropriate {@code visit} overload of this handler. Card drawing logic is
 * further delegated to {@link CardDrawer}, while legal and forced move
 * validation is handled by {@link MoveChecker}, both of which also leverage
 * the Visitor pattern internally.
 *
 * <p>The phase concludes when no more players remain on the offer track,
 * at which point the state machine transitions to {@link PreEndOfRoundPhase}.
 *
 * @see GamePhaseHandler
 * @see CardDrawer
 * @see MoveChecker
 * @see PreEndOfRoundPhase
 */
public class ActionPhase implements GamePhaseHandler {

    private final GameModel model;
    private Player currentPlayer;
    private OfferTileAction currentAction;

    public ActionPhase(GameModel model) {
        this.model = model;
    }

    @Override
    public void onEnter() {
        startNextPlayerTurn();
    }

    /**
     * Initiates the turn for the next player on the offer track.
     *
     * <p>Disconnected players are automatically skipped: their totem is
     * returned to the turn order before moving on. If no connected player
     * remains, the phase transitions to {@link PreEndOfRoundPhase}.
     *
     * <p>Once a valid player is identified, the {@link OfferTileAction} for
     * their tile is retrieved and activated. If the action is immediately
     * complete (e.g., empty rows) or no legal move exists, the turn advances
     * automatically without waiting for a player command.
     *
     * @throws IllegalStateException if a player's offer tile has no associated action
     */
    private void startNextPlayerTurn() {
        Board board = model.getBoard();

        // get the next player; if it's a disconnected one it skips him
        do {
            currentPlayer = board.getNextPlayerOnOfferTrack();
            if (currentPlayer == null) break;  // No more players
            if (!currentPlayer.isConnected()) {
                // Skip disconnected player and return their totem to the turn order
                board.disconnectedReturnTotemToTurnOrder(currentPlayer);
            }
        } while (!currentPlayer.isConnected());

        // Se non ci sono più giocatori, la fase Action è finita
        if (currentPlayer == null) {
            currentAction = null;
            model.setPhase(new PreEndOfRoundPhase(model));
            return;
        }

        currentAction = board.getOfferTrack()
                .getOccupiedTileByPlayer(currentPlayer)
                .getAction();

        // si può cancellare?
        if (currentAction == null) {
            throw new IllegalStateException("No action associated with the offer tile.");
        }

        model.notifyChange();

        currentAction.onEnterAction(currentPlayer, model);

        checkActionCompletionOrAutoAdvance();
    }


    /**
     * Processes a {@link DrawCardCommand} issued by the active player.
     *
     * <p>Resolves the target card by its ID, delegates draw validation to
     * {@link OfferTileAction#canDraw}, and executes the draw via
     * {@link CardDrawer}. After each draw, checks whether the turn should
     * advance automatically.
     *
     * @param cmd the command carrying the ID of the card the player intends to draw
     * @throws IllegalArgumentException if no card with the given ID exists on the board
     * @throws IllegalStateException    if the current offer tile does not permit
     *                                  drawing the specified card
     * @throws Exception                if an unexpected error occurs during draw processing
     */
    @Override
    public void visit(DrawCardCommand cmd) throws Exception {
        int cardId = cmd.cardId();
        //ensureActiveTurn();

        RowsManager rowsManager = model.getRowsManager();
        Card card = rowsManager.findCardById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card " + cardId + " not found on board"));

        if (!currentAction.canDraw(card, rowsManager)) {
            throw new IllegalStateException("The offer tile does not allow drawing this card (wrong row or draw limit reached)");
        }

        CardDrawer cardDrawer = new CardDrawer(currentPlayer, rowsManager, currentAction);
        cardDrawer.drawCard(card);

        model.notifyChange();

        // Dopo ogni pescata, verifichiamo se il turno è finito o deve essere forzatamente terminato
        checkActionCompletionOrAutoAdvance();
    }


    /**
     * Processes an {@link EndTurnCommand} issued by the active player.
     *
     * <p>A player may end their turn voluntarily only when no forced draws
     * remain (i.e., no acquirable character cards are left on the board).
     * If forced moves still exist, the command is rejected.
     *
     * @param cmd the end-turn command (currently unused beyond intent signalling)
     * @throws IllegalStateException if the player still has mandatory draws to complete
     * @throws Exception             if an unexpected error occurs during turn advancement
     */
    @Override
    public void visit(EndTurnCommand cmd) throws Exception {
        //ensureActiveTurn();

        if (hasAnyForcedMove()) {
            throw new IllegalStateException("All mandatory draws must be completed before ending the turn");
        }

        model.notifyChange();
        advanceActionTurn();
    }


    /**
     * Automatically advances the turn if the current action is finished or
     * no legal move is available.
     *
     * <p>An action is considered finished when the player has drawn the maximum
     * number of cards imposed by their offer tile. A legal move is absent when
     * neither a drawable character card nor an acquirable building card exists
     * in the accessible rows.
     */
    private void checkActionCompletionOrAutoAdvance() {
        if (currentAction.isFinished() || !hasAnyLegalMove()) {
            advanceActionTurn();
        }
    }


    /**
     * Determines whether at least one legal move is available to the current player.
     *
     * <p>A move is considered legal if the player can draw at least one character
     * card or acquire at least one building card from the accessible rows.
     * Delegates evaluation to {@link MoveChecker} via the Visitor pattern.
     *
     * @return {@code true} if at least one legal move exists; {@code false} otherwise
     */
    private boolean hasAnyLegalMove() {
        RowsManager rowsManager = model.getRowsManager();
        MoveChecker moveChecker = new MoveChecker(currentPlayer, currentAction, rowsManager);

        return moveChecker.checkLegalMoves(rowsManager.getAllCardsOnBoard());
    }


    /**
     * Determines whether at least one forced (mandatory) move remains for
     * the current player.
     *
     * <p>A forced move exists when at least one character card is still
     * drawable. The player may not voluntarily end their turn while forced
     * moves remain. Delegates evaluation to {@link MoveChecker}.
     *
     * @return {@code true} if a mandatory draw is still available; {@code false} otherwise
     */
    private boolean hasAnyForcedMove() {
        RowsManager rowsManager = model.getRowsManager();
        MoveChecker moveChecker = new MoveChecker(currentPlayer, currentAction, rowsManager);

        return moveChecker.checkForcedMoves(rowsManager.getAllTribeCardsOnBoard());
    }


    /**
     * Concludes the current player's turn and initiates the next one.
     *
     * <p>Returns the current player's totem to the turn order tile, resets
     * the phase's transient state ({@code currentPlayer} and
     * {@code currentAction}), then delegates to {@link #startNextPlayerTurn()}.
     */
    private void advanceActionTurn() {
        model.getBoard().returnTotemToTurnOrder(currentPlayer);

        currentPlayer = null;
        currentAction = null;

        // Restart the cycle for the next player.
        startNextPlayerTurn();
    }


    /**
     * Forcibly skips the current player's turn due to disconnection.
     *
     * <p>Returns the disconnected player's totem via the disconnection-specific
     * path (which may apply different ordering rules than a normal return),
     * resets transient state, and immediately starts the next player's turn.
     */
    @Override
    public void skipCurrentPlayerTurn() {
        model.getBoard().disconnectedReturnTotemToTurnOrder(currentPlayer);
        currentPlayer = null;
        currentAction = null;
        startNextPlayerTurn();
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
