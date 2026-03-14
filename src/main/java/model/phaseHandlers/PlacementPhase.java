package model.phaseHandlers;

import model.GameModel;
import model.board.OfferTile;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.enums.TotemLocation;
import model.player.Player;

import java.util.List;

public class PlacementPhase implements GamePhaseHandler {
    private final GameModel model;

    private List<Player> turnOrder;
    private int currentIndex;
    private Player currentPlayer;

    public PlacementPhase(GameModel model) {
        this.model = model;
    }

    @Override
    public void onEnter() {
        turnOrder = model.getBoard().getTurnOrderTile().getTurnOrder();
        currentIndex = 0;
        currentPlayer = turnOrder.get(currentIndex);

        model.notifyChange("placement_started:" + currentPlayer.getName());
    }

    @Override
    public void placeTotem(Player player, OfferTile offerTile) throws Exception {
        if (!canPlaceTotem(player, offerTile)) {
            // build a meaningful error message
            if (player != currentPlayer) {
                throw new Exception("It's not your turn! Waiting for " + currentPlayer.getName());
            }
            if (player.getTotem().getLocation() != TotemLocation.TURN_ORDER_TILE) {
                throw new Exception("Your totem is already on the offer track.");
            }
            if (tile.isOccupied()) {
                throw new Exception("That offer tile is already occupied.");
            }
            throw new Exception("Invalid placement.");
        }

        // delegate physical placement to board
        model.getBoard().placeTotem(player.getTotem(), offerTile);

        model.notifyChange("totem_placed:" + player.getName());

        advanceTurn();
    }

    // TODO da rivedere
    @Override
    public boolean canPlaceTotem(Player player, OfferTile tile) throws Exception{
        //player is currentPlayer
        if(player != currentPlayer){
            throw new Exception("It's not your turn!");
        }

        //currentPhase== PLACEMENT;
        if(currentPhase != GamePhase.PLACEMENT){
            throw new Exception("It's not the placement phase!");
        }

        //player.getTotem().getLocation() == TotemLocation.TURN_ORDER_TILE;
        if(currentPlayer.getTotem().getLocation() != TotemLocation.TURN_ORDER_TILE){
            throw new Exception("Your totem is already on the offer track");
        }

    }

    private void advanceTurn() {
        currentIndex++;

        if (currentIndex < turnOrder.size()) {
            // next player's turn to place
            currentPlayer = turnOrder.get(currentIndex);
            model.notifyChange("turn_changed:" + currentPlayer.getName());
        } else {
            // all totems placed → move to action phase
            model.setPhase(new ActionPhase(model));
        }
    }

    @Override
    public void chooseColor(Player player, TotemColor color) throws IllegalStateException {

    }

    @Override
    public boolean canAct(Player player, Object target) {
        return false;
    }

    @Override
    public GamePhase getPhase() {
        return null;
    }

    @Override
    public Player getCurrentPlayer() {
        return null;
    }

}
