package model.phaseHandlers;

import model.GameModel;
import model.cards.Card;
import model.enums.GamePhase;
import model.player.Player;
import model.rowsManager.RowsManager;
import shared.command.gameCommand.DrawCardCommand;
import shared.command.gameCommand.EndTurnCommand;

import java.util.Optional;

public class PreEndOfRoundPhase implements GamePhaseHandler {

    private final GameModel model;
    private Player activePlayer;

    public PreEndOfRoundPhase(GameModel model) {
        this.model = model;
    }

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
        model.notifyChange();
    }

    /**
     * @implNote Draw a card from the top row.
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
     * @implNote End the turn without drawing a card, if the player decides not to use the extra draw.
     * This will transition directly to the EndOfRoundPhase.
     */
    @Override
    public void visit(EndTurnCommand cmd) throws Exception {
        if (activePlayer != null) {
            // notificare che il player ha deciso di non pescare?
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
