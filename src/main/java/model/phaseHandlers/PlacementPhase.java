package model.phaseHandlers;

import model.GameModel;
import model.board.Board;
import model.board.OfferTile;
import model.enums.GamePhase;
import model.enums.TotemLocation;
import model.player.Player;
import shared.command.gameCommand.PlaceTotemCommand;

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
        turnOrder = model.getTurnOrder();
        currentIndex = 0;
        currentPlayer = turnOrder.get(currentIndex);

        if (!currentPlayer.isConnected()) {
            advanceTurn();
            return;
        }

        model.notifyChange();
    }

    @Override
    public void visit(PlaceTotemCommand cmd) throws Exception {
        Player player = model.getPlayerByName(cmd.playerName());
        char tileId = cmd.tileId();

        Board board = model.getBoard();
        OfferTile offerTile = board.findTileByLetter(tileId);
        if (offerTile == null) {
            throw new IllegalArgumentException("tileId " + tileId + " is invalid");
        }

        if (player != currentPlayer) {
            //checked also in gameController
            throw new IllegalArgumentException("It's not " + player.getName() + "'s turn to place a totem");
        }
        if (currentPlayer.getLocation() != TotemLocation.TURN_ORDER_TILE) {
            throw new IllegalArgumentException("Player " + player.getName() + " cannot place a totem because he is not on the turn order tile");
        }
        if (offerTile.isOccupied()) {
            throw new IllegalArgumentException("Tile " + offerTile.getLetter() + " is already occupied");
        }

        board.placeTotem(player, offerTile);
        advanceTurn();
    }

    private void advanceTurn() {
        // get the next player; if it's a disconnected one it skips him
        do {
            currentIndex++;
        } while (currentIndex < turnOrder.size() && !turnOrder.get(currentIndex).isConnected());

        if (currentIndex < turnOrder.size()) {
            // next player's turn to place
            currentPlayer = turnOrder.get(currentIndex);
            model.notifyChange();
        } else {
            for(Player p: model.getPlayers()) {
                if (!p.isConnected()) {
                    model.getBoard().getTurnOrderTile().freeSlot(p);
                    model.getBoard().getTurnOrderTile().disconnectedReturnTotemAndResolveEffects(p);
                }
            }
            // all totems placed → move to action phase
            model.setPhase(new ActionPhase(model));
            model.notifyChange();
        }
    }

    @Override
    public void skipCurrentPlayerTurn() {
        advanceTurn();
    }

    @Override
    public GamePhase getPhase() { return GamePhase.PLACEMENT; }

    @Override
    public Player getCurrentPlayer() { return currentPlayer; }
}
