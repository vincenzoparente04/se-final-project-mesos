package model.phaseHandlers;

import model.buildingEffects.OnCharacterAcquiredEffects.OnAcquireBuildingEffect;
import model.cards.Card;
import model.cards.buildingCards.BuildingCard;
import model.cards.charachterCards.CharacterCard;
import model.cards.eventCards.EventCard;
import model.cards.eventCards.SustenanceEventCard;
import model.player.Player;
import model.rowsManager.CardVisitor;
import model.rowsManager.RowsManager;

public class CardDrawer implements CardVisitor {
    private Player player;
    private RowsManager rowsManager;

    CardDrawer(Player player, RowsManager rowsManager) {
        this.player = player;
        this.rowsManager = rowsManager;
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
            throw new IllegalStateException("Non hai i requisiti per prendere questa carta (Cibo insufficiente o è un Evento).");
        }else {
            rowsManager.removeCard(card.getId());

            player.removeFood(card.getDiscountedCost(player), 0);
            card.registerToTribe(player);
        }
    }

    // Event cards can't be drawn
    @Override
    public void visit(EventCard card) {
        throw new IllegalStateException("Non hai i requisiti per prendere questa carta (Cibo insufficiente o è un Evento).");
    }

    @Override
    public void visit(SustenanceEventCard card) {
        throw new IllegalStateException("Non hai i requisiti per prendere questa carta (Cibo insufficiente o è un Evento).");
    }
}
