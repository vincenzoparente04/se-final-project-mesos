package model.phaseHandlers;

import model.GameModel;
import model.board.OfferTile;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.enums.TotemLocation;
import model.player.Player;

import java.util.List;

public class PlacementPhase extends GamePhaseHandler {

    private List<Player> turnOrder;
    private int currentIndex;
    private Player currentPlayer;

    public PlacementPhase(GameModel model) {
        super(model);
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
        try{
            canPlaceTotem(player, offerTile);
        } catch (Exception e){

        }
        model.getBoard().placeTotem(player, offerTile);
        model.notifyChange("totem_placed:" + player.getName());
        advanceTurn();
    }

    // TODO da rivedere
    public void canPlaceTotem(Player player, OfferTile tile) throws Exception{
        if(player != currentPlayer){
            throw new Exception("It's not your turn! Waiting for " + currentPlayer.getName());
        }

        if(model.getCurrentPhase() != GamePhase.PLACEMENT){
            throw new Exception("It's not the placement phase!");
        }

        if(currentPlayer.getLocation() != TotemLocation.TURN_ORDER_TILE){
            throw new Exception("Your totem is already on the offer track");
        }

        if (tile.isOccupied()) {
            throw new Exception("That offer tile is already occupied.");
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
    public GamePhase getPhase() {
        return model.getCurrentPhase();
    }

    @Override
    public Player getCurrentPlayer() {
        return model.getCurrentPlayer();
    }

}
