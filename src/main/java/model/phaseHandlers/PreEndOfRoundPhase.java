package model.phaseHandlers;

import model.GameModel;
import model.board.OfferTileAction.DrawCardsAction;
import model.board.OfferTileAction.OfferTileAction;
import model.cards.Card;
import model.enums.GamePhase;
import model.player.Player;
import model.rowsManager.RowsManager;
import shared.command.gameCommand.DrawCardCommand;
import shared.command.gameCommand.EndTurnCommand;

import java.util.Optional;

/**
 * Represents a specialized transitional phase preceding the end of a round,
 * dedicated exclusively to resolving the "extra draw" bonus mechanic.
 *
 * <p>This handler dynamically evaluates the game state to determine if any player
 * is eligible for a bonus action. Its lifecycle and responsibilities include:
 * <ol>
 * <li><b>Evaluation:</b> Upon entry, it identifies if a player has the extra draw
 * capability and assesses the board for valid targets using a {@link MoveChecker}.
 * If no eligible player exists, or if no valid moves remain, it short-circuits
 * and immediately transitions to the {@link EndOfRoundPhase}.</li>
 * <li><b>Execution:</b> If active, it awaits a {@link DrawCardCommand} or an
 * {@link EndTurnCommand}. It enforces strict domain rules, specifically restricting
 * the bonus draw exclusively to the top row of the board.</li>
 * <li><b>Resolution:</b> After the player either successfully draws a valid card
 * (via {@link CardDrawer}), voluntarily passes, or is forcibly skipped due to a
 * network disconnection, the state machine guarantees a safe transition to the
 * next phase.</li>
 * </ol>
 * </p>
 *
 * @see GamePhaseHandler
 * @see model.board.OfferTileAction.DrawCardsAction
 * @see model.phaseHandlers.MoveChecker
 * @see model.phaseHandlers.CardDrawer
 */
public class PreEndOfRoundPhase implements GamePhaseHandler {

    private final GameModel model;
    private Player activePlayer;

    public PreEndOfRoundPhase(GameModel model) {
        this.model = model;
    }

    /**
     * Initializes the phase by checking for extra draw eligibility and validating
     * the board state.
     * <p>
     * <b>Short-Circuit Optimization:</b> It constructs a temporary {@link DrawCardsAction}
     * to perform a dry-run against the current board using a {@code MoveChecker}.
     * If the evaluation yields no legal moves, the phase bypasses all client interaction
     * and synchronously delegates control to the {@code EndOfRoundPhase}.
     * </p>
     */
    @Override
    public void onEnter() {
        activePlayer = model.getPlayers().stream()
                .filter(Player::hasExtraDraw)
                .findFirst()
                .orElse(null);

        // offer tile action created to represent the extra draw action
        OfferTileAction extraDrawAction = new DrawCardsAction(1, 0);
        RowsManager rowsManager = model.getRowsManager();
        MoveChecker moveChecker = new MoveChecker(activePlayer, extraDrawAction, rowsManager);

        if (activePlayer == null || !(moveChecker.checkLegalMoves(rowsManager.getAllCardsOnBoard()))) {
            model.setPhase(new EndOfRoundPhase(model));
            return;
        }

        model.notifyChange();
    }

    /**
     * Intercepts and validates a targeted card draw request during the bonus phase.
     * <p>
     * This method strictly enforces the phase-specific domain rule that bonus draws
     * must originate from the top row of the board. Upon successful validation, it
     * delegates the actual state mutation to a {@link CardDrawer} and subsequently
     * advances the game state.
     * </p>
     *
     * @param cmd the payload containing the ID of the requested card.
     * @throws IllegalStateException if the active player is null, the card does not exist,
     * or the requested card is not located in the top row.
     * @throws Exception if an unexpected error occurs during the drawing transaction.
     */
    @Override
    public void visit(DrawCardCommand cmd) throws Exception {
        int cardId = cmd.cardId();
        if (activePlayer == null) return;

        RowsManager rowsManager = model.getRowsManager();
        Optional<Card> found = rowsManager.findCardById(cardId);

        if (found.isEmpty()) {
            throw new IllegalStateException("Card " + cardId + " not found");
        }
        if (!rowsManager.topRowContainsCard(cardId)) {
            throw new IllegalStateException("Card " + cardId + " is not in the top row and cannot be drawn");
        }

        CardDrawer cardDrawer = new CardDrawer(activePlayer, rowsManager, null);
        cardDrawer.drawCard(found.get());

        model.setPhase(new EndOfRoundPhase(model));
    }

    /**
     * Processes a voluntary forfeiture of the bonus draw.
     * <p>
     * If the active player chooses not to utilize their extra action, this method
     * intercepts the end-turn command and smoothly advances the state machine directly
     * to the {@link EndOfRoundPhase}.
     * </p>
     *
     * @param cmd the command indicating the player's intent to pass.
     */
    @Override
    public void visit(EndTurnCommand cmd) throws Exception {
        if (activePlayer != null) {
            model.setPhase(new EndOfRoundPhase(model));
        }
    }

    @Override
    public void skipCurrentPlayerTurn()
    {
        activePlayer = null;
        model.setPhase(new EndOfRoundPhase(model));
        this.model.notifyChange();
    }

    @Override
    public GamePhase getPhase() { return GamePhase.PRE_END_OF_ROUND; }

    @Override
    public Player getCurrentPlayer() { return activePlayer; }
}
