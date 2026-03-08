package controller.round;

import model.GameModel;

public class TotemReturnHandler {

    private final GameModel model;

    public void returnTotem(Player player)
    // 1. take player's totem
    // 2. find first available slot in TurnOrderTile
    // 3. place the totem in the slot
    // 4. totem.setLocation(TURN_ORDER_TILE)
    // 5. apply food bonus if present:
    //    int bonus = turnOrderTile.getFoodBonusForSlot(slotIndex)
    //    if (bonus > 0) player.addFood(bonus)
    // 6. if it is the last slot: applyLastPlacePenalty(player)
    // 7. model.notifyChange("totem_returned")

    private void applyLastPlacePenalty(Player player)
    // if player.hasFood(1): player.removeFood(1)
    // otherwise: player.removePrestigePoints(2)
    // REFACTOR: si può migliorare levando questo attributo da player
}