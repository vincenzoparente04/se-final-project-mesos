package controller.round;

import model.GameModel;

public class CardAcquisitionHandler {

    private final GameModel model;
    private final CostCalculator costCalculator;

    public void acquireCard(Player player, Card card)
    // different based on card's type:
    // if CharacterCard -> acquireCharacter(player, card)
    // if BuildingCard  -> acquireBuilding(player, card)

    private void acquireCharacter(Player player, CharacterCard card)
    // 1. removes the card from row (TopRow or BottomRow)
    // 2. adds card to the tribe: player.getTribe().addCharacter(card)
    // 3. check if the card has immediate effects:
    //    ImmediateEffect effect = card.getImmediateEffect()
    //    if (effect != null) effect.apply(player, model)
    // 4. model.notifyChange("card_acquired")

    private void acquireBuilding(Player player, BuildingCard card)
    // 1. calculate the effective cost:
    //    int cost = costCalculator.calculateBuildingCost(player, card)
    // 2. player.removeFood(cost)
    // 3. removes the card from the row
    // 4. add the cart to the tribe: player.getTribe().addBuilding(card)
    // 5. model.notifyChange("card_acquired")
    // nota: le BuildingCard non hanno effetti immediati —
    //       i loro effetti si attivano durante gli eventi o a fine partita
}