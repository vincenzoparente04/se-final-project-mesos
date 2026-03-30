package model.phaseHandlers;

import model.board.OfferTileAction.OfferTileAction;
import model.cards.Card;
import model.cards.TribeCard;
import model.cards.buildingCards.BuildingCard;
import model.cards.charachterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.player.Player;
import model.rowsManager.CardVisitor;
import model.rowsManager.RowsManager;

import java.util.List;

public class MoveChecker implements CardVisitor {
    private boolean result;
    private Player player;
    private OfferTileAction currentAction;
    private RowsManager rowsManager;

    public MoveChecker(Player currentPlayer, OfferTileAction currentAction, RowsManager rowsManager) {
        this.result = false;
        this.player = currentPlayer;
        this.currentAction = currentAction;
        this.rowsManager = rowsManager;
    }

    public boolean checkForcedMoves(List<TribeCard> allTribeCardsOnBoard) {
        for (TribeCard card : allTribeCardsOnBoard) {
            if (result) {return true;} //if result is already true, it means that the player has at least one legal move, so we can stop checking
            if (currentAction.canDraw(card, rowsManager)) {
                card.accept(this);
            }
        }
        return result;
    }

    public boolean checkLegalMoves(List<Card> allCardsOnBoard) {
        for (Card card : allCardsOnBoard) {
            if (result) {return true;} //if result is already true, it means that the player has at least one legal move, so we can stop checking
            if (currentAction.canDraw(card, rowsManager)) {
                card.accept(this);
            }
        }
        return result;
    }

    @Override
    public void visit(CharacterCard card) {
        result = true;
    }

    @Override
    public void visit(EventCard card) {
        //it's not a legal move
    }

    @Override
    public void visit(SustenanceEventCard card) {
        //it's not a legal move
    }

    @Override
    public void visit(BuildingCard card) {
        //it's a legal move only if the player has enough food to pay for it
        if (player.getFood() >= card.getDiscountedCost(player)) {
            result = true;
        }
    }
}
