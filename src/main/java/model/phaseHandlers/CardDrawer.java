package model.phaseHandlers;

import model.board.OfferTileAction.OfferTileAction;
import model.cards.buildingCards.buildingEffects.onCharacterAcquiredEffects.OnAcquireBuildingEffect;
import model.cards.Card;
import model.cards.buildingCards.BuildingCard;
import model.cards.characterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.player.Player;
import model.rowsManager.CardVisitor;
import model.rowsManager.RowsManager;

public class CardDrawer implements CardVisitor {
    private Player player;
    private RowsManager rowsManager;
    private OfferTileAction currentAction;

    CardDrawer(Player player, RowsManager rowsManager, OfferTileAction currentAction) {
        this.player = player;
        this.rowsManager = rowsManager;
        this.currentAction = currentAction;
    }

    /**
     * @implNote Uses visitor pattern to customize the drawing process of the card based on its type.
     * Character cards are added to the tribe and trigger OnAcquire effects,
     * while building cards check for food cost and apply builder discounts before being added to the tribe.
     * Event cards cannot be drawn and will throw an exception if attempted.
     * @param card card to be drawn
     */
    public void drawCard(Card card) {
        card.accept(this);
    }


    @Override
    public void visit(CharacterCard card) {
        if (currentAction != null) { // null when called by PreEndOfRoundPhase (no tile action involved)
            currentAction.performDraw(card, rowsManager);
        }

        rowsManager.removeCard(card.getId());

        card.registerToTribe(player);

        // checks for OnAcquire effects
        for (OnAcquireBuildingEffect effect : player.getTribe().getOnAcquireBuildingEffects()) {
            effect.applyEffect(player);
        }
    }

    @Override
    public void visit(BuildingCard card) {
        if (player.getFood() < card.getDiscountedCost(player)) {
            throw new IllegalStateException("Insufficient food to acquire building card.");
        } else {
            if (currentAction != null) { // null when called by PreEndOfRoundPhase (no tile action involved)
                currentAction.performDraw(card, rowsManager);
            }

            rowsManager.removeCard(card.getId());

            player.removeFood(card.getDiscountedCost(player));
            card.registerToTribe(player);
        }
    }

    // Event cards cannot be drawn by players
    @Override
    public void visit(EventCard card) {
        throw new IllegalStateException("Event cards cannot be drawn.");
    }

    @Override
    public void visit(SustenanceEventCard card) {
        throw new IllegalStateException("Event cards cannot be drawn.");
    }
}
