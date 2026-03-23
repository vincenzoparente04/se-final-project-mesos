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
    public void placeTotem(Player player, OfferTile offerTile) {
        if(!canPlaceTotem(player, offerTile)) { return; };

        try {
            model.getBoard().placeTotem(player, offerTile);
            model.notifyChange("totem_placed:" + player.getName());
            advanceTurn();
        } catch (Exception e) {
            // solo errori tecnici imprevisti della board
            model.notifyChange("error:move_failed");
        }
    }


    public boolean canPlaceTotem(Player player, OfferTile tile){
        if(player != currentPlayer) return false;
        //if(model.getCurrentPhase() != GamePhase.PLACEMENT) return false;
        if(currentPlayer.getLocation() != TotemLocation.TURN_ORDER_TILE) return false;
        if (tile.isOccupied()) return false;

        return true;
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
    public GamePhase getPhase() { return GamePhase.PLACEMENT; }

    @Override
    public Player getCurrentPlayer() { return currentPlayer; }
}