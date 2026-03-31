package model.board;

import model.factories.BoardFactory;
import model.player.Player;

public class Board {
    private final OfferTrack offerTrack;
    private final TurnOrderTile turnOrderTile;

    public Board() {  // TODO: check how we want to construct the board
        this.offerTrack = new OfferTrack();
        this.turnOrderTile = new TurnOrderTile();
    }

    public void setup(int playerCount) {
        BoardFactory.BoardComponents components = BoardFactory.createComponents(playerCount);
        offerTrack.setup(components.offerTiles());
        turnOrderTile.setup(components.turnOrderSlots(), components.turnOrderTileImage());
    }

    /**
     * Delegate the placeTotem implementation to the offerTrack(which delegates to OfferTile)
     * @param player
     * @param offerTile
     * @throws Exception
     */
    public void placeTotem(Player player, OfferTile offerTile) {
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
        return offerTrack.getNextPlayer();
    }
}
