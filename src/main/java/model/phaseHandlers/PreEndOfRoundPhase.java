package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.cards.Card;
import model.enums.GamePhase;
import model.player.Player;

public class PreEndOfRoundPhase extends GamePhaseHandler {

    private Player activePlayer;

    public PreEndOfRoundPhase(GameModel model) { super(model); }

    /**
     * @implNote Find the player who can have an extra draw.
     */
    @Override
    public void onEnter() {
        activePlayer = model.getPlayers().stream()
                .filter(Player::hasExtraDraw)
                .findFirst()
                .orElse(null);

        if (activePlayer == null) {
            model.setPhase(new EndOfRoundPhase(model));
            return;
        }
    }

    /**
     * @implNote Draw a card from the top row.
     * @param cardId
     */
    @Override
    public void drawCard(int cardId) {
        if (activePlayer == null) return;

        Board board = model.getBoard();
        Card card = board.findCardById(cardId);

        if (card == null || !board.topRowContainsCard(cardId)) {
            // è necessario notificare l'erorre?
            return;
        }

        if (!card.canBeAcquiredBy(activePlayer, model)) {
            return;
        }

        board.removeCard(cardId);
        card.acquiredBy(activePlayer, model);

        model.setPhase(new EndOfRoundPhase(model));
    }

    // l 'effetto è facoltativo quindi il player potrebbe anche non pescare la carta extra
    public void skipAction() {
        if (activePlayer != null) {
            // notificare che il player ha deciso di non pescare?
            model.setPhase(new EndOfRoundPhase(model));
        }
    }

    @Override
    public GamePhase getPhase() { return GamePhase.PRE_END_OF_ROUND; }

    @Override
    public Player getCurrentPlayer() { return activePlayer; }
}