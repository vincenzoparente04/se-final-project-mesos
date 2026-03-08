package controller.support;

import model.GameModel;

public class ActionValidator {

    private final GameModel model;
    private final CostCalculator costCalculator;

    public boolean validateTotemPlacement(Player player, OfferTile tile)
    // is the slot free? tile.isOccupied() == false
    // is this player turn (fase PLACEMENT)
    // is the Totem on TurnOrderTile (unplaced)? etc ...

    public boolean validateCardSelection(Player player, Card card, OfferTile tile)
    // the card is in the row indicated by the OfferTile?
    //   if card is in TopRow -> tile.getAction().getTopRowCards() > 0
    //   if card is in BottomRow -> tile.getAction().getBottomRowCards() > 0?
    // if it is a BuildingCard: player.hasFood(costCalculator.calculateBuildingCost())
    // etc......

    public boolean validateCardSelection(Player player, Card card, OfferTile tile)
}