package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.board.OfferTile;
import model.enums.GamePhase;
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
        turnOrder = model.getTurnOrder();
        currentIndex = 0;
        currentPlayer = turnOrder.get(currentIndex);

        model.notifyChange("placement_started:" + currentPlayer.getName());
    }

    @Override
    public void placeTotem(Player player, char tileId) {
        Board board = model.getBoard();
        OfferTile offerTile = board.findTileByLetter(tileId);
        if(!canPlaceTotem(player, offerTile)) { return; };

        board.placeTotem(player, offerTile);
        advanceTurn();
    }


    public boolean canPlaceTotem(Player player, OfferTile tile){
        if(player != currentPlayer) return false;
        //if(model.getCurrentPhase() != GamePhase.PLACEMENT) return false;
        if(currentPlayer.getLocation() != TotemLocation.TURN_ORDER_TILE) return false;
        return !tile.isOccupied();
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
            model.notifyChange("pahse_changed.");
        }
    }

    @Override
    public GamePhase getPhase() { return GamePhase.PLACEMENT; }

    @Override
    public Player getCurrentPlayer() { return currentPlayer; }
}