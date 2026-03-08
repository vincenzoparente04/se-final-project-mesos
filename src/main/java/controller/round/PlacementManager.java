package controller.round;

import model.GameModel;

public class PlacementManager {

    private final GameModel model;
    private final GameView view;
    private final ActionValidator validator;

    public void runPlacementPhase()
    // for every player in the turn order:
    //   1. view.promptTotemPlacement(player) -> receives the selected tile
    //   2. validator.validateTotemPlacement(player, tile) -> is it available?
    //   3. if valid: place the Totem on the tile
    //      update totem.setLocation(OFFER_TRACK)
    //      tile.placeTotem(totem)
    //   4. model.notifyChange("totem_placed")

    private List<Player> getPlacementOrder()
    // reads the order from the TurnOrderTile:
    // turnOrderTile.getTurnOrder() -> list of Totems -> list of Players
}
