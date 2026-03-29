package model.phaseHandlers;

import model.GameModel;
import model.cards.Card;
import model.enums.GamePhase;
import model.player.Player;
import model.rowsManager.RowsManager;

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
        CardDrawer cardDrawer = new CardDrawer(activePlayer, model.getRowsManager(), null);

        if (activePlayer == null) return;

        RowsManager rowsManager = model.getRowsManager();
        Card card = rowsManager.findCardById(cardId);

        if (card == null || !rowsManager.topRowContainsCard(card.getId())) {
            // è necessario notificare l'erorre?
            return;
        }

        cardDrawer.drawCard(card);

        model.setPhase(new EndOfRoundPhase(model));
    }

    /**
     * @implNote End the turn without drawing a card, if the player decides not to use the extra draw.
      * This will transition directly to the EndOfRoundPhase.
     */
    @Override
    public void endTurn() {
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