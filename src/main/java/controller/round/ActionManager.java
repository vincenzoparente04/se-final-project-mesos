package controller.round;

import model.GameModel;


public class ActionManager {
    private final GameModel model;
    private final GameView view;
    private final CardAcquisitionHandler cardAcquisitionHandler;
    private final TotemReturnHandler totemReturnHandler;
    private final ActionValidator validator;

    public void runActionPhase()
    // for every totem on the OfferTrack in order from left to right:
    //   1. get the player associated to the totem and the tile it occupies
    //   2. resolve the action of the tile
    //   3. return the totem to the TurnOrderTile

    private void resolvePlayerAction(Player player, OfferTile tile)
    // reads the action of the tile (OfferTileAction)
    // if foodOnly (tile A): player.addFood(tile.getAction().getFoodAmount())
    // otherwise:
    //   ask the view the nb of cards the player wants to take from the top and bottom row (up to the maximum allowed by the tile action)
    //   for every selected card:
    //     validator.validateCardSelection(player, card, tile) → is it from the correct row?
    //     cardAcquisitionHandler.acquireCard(player, card)
    // then: totemReturnHandler.returnTotem(player)

    private List<Player> getActionOrder()
    // reads the totems on the OfferTrack in order from left to right:
    // offerTrack.getOccupiedTilesInOrder() → for every tile → tile.getOccupant().getOwner()
}