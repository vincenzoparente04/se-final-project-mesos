package model.phaseHandlers;

import model.GameModel;
import model.board.OfferTileAction;
import model.cards.Card;
import model.enums.GamePhase;
import model.enums.TotemLocation;
import model.player.Player;

public class ActionPhase extends GamePhaseHandler {

    private Player currentPlayer;
    private int cardsDrawnFromTopRow;
    private int cardsDrawnFromBottomRow;

    public ActionPhase(GameModel model) {
        super(model);
    }

    @Override
    public void onEnter() {
        currentPlayer = model.getBoard().getOfferTrack().getOccupiedTilesInOrder().getFirst().getOccupant().getOwner();
        resetDrawCounters();

        model.notifyChange("action_started:" + currentPlayer.getName());
    }

    public void drawCard(int cardId) {
        if (!canDrawCard(cardId)) return;

        // aggiorna il contatore della riga corretta
        if (model.getBoard().getTopRow().containsCard(cardId)) {
            cardsDrawnFromTopRow++;
        } else {
            cardsDrawnFromBottomRow++;
        }

        // rimuove la carta dal tabellone
        Card card = model.getBoard().findCardById(cardId);
        model.getBoard().removeCard(cardId);
        currentPlayer.getTribe().addCharacter(card); // TODO capire come gestire i tipi qui
        applyImmediateEffect(card);
        notifyChange("card_drawn");

        if (hasCurrentPlayerFinishedDrawing()) {
            advanceActionTurn();
        }
    }

    public boolean canDrawCard(int cardId) {
        // TODO REFACTOR: aggiungi controllo per non pescare carte evento

        if (model.getBoard().findCardById(cardId) == null) return false;

        // la carta si trova in una riga da cui il giocatore può ancora pescare in questo turno?
        OfferTileAction action = getCurrentPlayerAction();

        if (model.getBoard().getTopRow().containsCard(cardId)) {
            return cardsDrawnFromTopRow < action.getTopRowCards();
        }
        if (model.getBoard().getBottomRow().containsCard(cardId)) {
            return cardsDrawnFromBottomRow < action.getBottomRowCards();
        }

        return false;
    }

    private boolean hasCurrentPlayerFinishedDrawing() {
        OfferTileAction action = getCurrentPlayerAction();

        // ha ancora carte da prendere dalla TopRow?
        boolean stillNeedsTop = cardsDrawnFromTopRow < action.getTopRowCards()
                && model.getBoard().getTopRow().hasAvailableCards();

        // ha ancora carte da prendere dalla BottomRow?
        boolean stillNeedsBottom = cardsDrawnFromBottomRow < action.getBottomRowCards()
                && model.getBoard().getBottomRow().hasAvailableCards();

        // il turno è finito quando non ha più nulla da pescare
        return !stillNeedsTop && !stillNeedsBottom;
    }

    public OfferTileAction getCurrentPlayerAction() {
        return model.getBoard().getOfferTrack()
                .getOccupiedTileByPlayer(currentPlayer)
                .getAction();
    }

    private void applyImmediateEffect(Card card) {
        // TODO
    }

    private void advanceActionTurn() {
        // return current player's totem to the turn order tile
        model.getBoard().getTurnOrderTile().returnTotem(currentPlayer);

        resetDrawCounters();

        // find the next player still on the offer track
        Player next = getNextPlayerOnOfferTrack();

        if (next != null) {
            currentPlayer = next;
            model.notifyChange("turn_changed:" + currentPlayer.getName());
        } else {
            // no more totems on the offer track → round is over
            model.setPhase(new PreEndOfRoundPhase(model));
        }
    }

    private void resetDrawCounters() {
        cardsDrawnFromTopRow = 0;
        cardsDrawnFromBottomRow = 0;
    }

    private Player getNextPlayerOnOfferTrack() {
        return model.getBoard().getOfferTrack()
                .getOccupiedTilesInOrder()
                .stream()
                .map(tile -> tile.getOccupant().getOwner())
                .filter(p -> p.getTotem().getLocation() == TotemLocation.OFFER_TRACK)
                .findFirst()
                .orElse(null);
    }

    @Override
    public GamePhase getPhase() {
        return model.getCurrentPhase();
    }

    @Override
    public Player getCurrentPlayer() {
        return model.getCurrentPlayer();
    }

}
