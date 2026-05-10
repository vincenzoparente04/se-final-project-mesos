package model.board;

import model.factories.BoardFactory;
import model.player.Player;

import java.util.List;
import java.util.Collections;

public class Board {
    private OfferTrack offerTrack;
    private TurnOrderTile turnOrderTile;

    public void setup(int playerCount) {
        BoardFactory.BoardComponents components = BoardFactory.createComponents(playerCount);
        this.offerTrack = new OfferTrack(components.offerTiles());
        this.turnOrderTile = new TurnOrderTile(components.turnOrderSlots(), components.turnOrderTileImage());
    }

    /**
     * Delegate the placeTotem implementation to the offerTrack(which delegates to OfferTile)
     * @param player
     * @param offerTile
     * @throws Exception
     */
    public void placeTotem(Player player, OfferTile offerTile) {
        turnOrderTile.freeSlot(player);
        offerTrack.placeTotem(player, offerTile);
    }

    // helpers:
    //private void populateBottomRow(TribeDeck tribeDeck, int playerCount){
    //    tribeDeck.draw(); // in loop
    //}
    //private void populateTopRow(TribeDeck tribeDeck, BuildingDeck buildingDeckI, int playerCount){
    //    tribeDeck.draw(); // in loop
    //    buildingDeckI.drawAll();
    //}

    /**
     * @implNote Find the correct tile corresponding to the char passed by the controller
     * @param letter
     * @return
     */
    public OfferTile findTileByLetter(char letter) {
        return offerTrack.getTileByLetter(letter);
    }

    public OfferTrack getOfferTrack() {
        return offerTrack;
    }


    public TurnOrderTile getTurnOrderTile() {
        return turnOrderTile;
    }

    public Player getNextPlayerOnOfferTrack() {
        return offerTrack != null ? offerTrack.getNextPlayer() : null;
    }

    /**
     * Removes the player's totem from the offer track and places it on the
     * first free turn-order slot, applying that slot's food/prestige effect.
     * This is the single call that ends an action turn atomically.
     */
    public void returnTotemToTurnOrder(Player player) {
        offerTrack.removeTotem(player);
        turnOrderTile.returnTotemAndResolveEffects(player);
    }

    public List<Player> getTurnOrder() {
        return turnOrderTile != null ? turnOrderTile.getTurnOrder() : Collections.emptyList();
    }

    public List<OfferTile> getOfferTiles() {
        return offerTrack != null ? offerTrack.getTiles() : Collections.emptyList();
    }

    public List<TurnOrderSlot> getTurnOrderSlots() {
        return turnOrderTile != null ? turnOrderTile.getSlots() : Collections.emptyList();
    }
}
