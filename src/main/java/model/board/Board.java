package model.board;

// The main board class that contains all the components of the board and provides access to them for the Controller and the View.
// It is only responsible for holding the state of the board and providing access to its components, while the Controller is
// responsible for applying the game logic and mutating the state of the board accordingly; the View is responsible for
// displaying the current state of the board.

import model.cards.CharacterCard;
import model.deck.BuildingDeck;
import model.deck.TribeDeck;
import model.player.Player;
import model.player.Totem;

public class Board {
    private final OfferTrack offerTrack;
    private final TurnOrderTile turnOrderTile;
    private TopRow topRow;
    private BottomRow bottomRow;

    public OfferTrack getOfferTrack() {
        return offerTrack;
    }

    public TurnOrderTile getTurnOrderTile() {
        return turnOrderTile;
    }

    public void setupBoard(TribeDeck tribeDeck, BuildingDeck buildingDeckEraI, int playerCount){
        turnOrderTile.setup(playerCount);
        offerTrack.setup(playerCount);
        populateBottomRow(tribeDeck, playerCount);
        populateTopRow(tribeDeck, buildingDeckEraI, playerCount);
    }

    /**
     * @implNote delegate the placeTotem implementation to the offerTrack
     * @param totem
     * @param offerTile
     * @throws Exception
     */
    public void placeTotem(Totem totem, OfferTile offerTile) throws Exception {
        offerTrack.placeTotem(totem, offerTile);
    }

    // helpers:
    private void populateBottomRow(TribeDeck tribeDeck, int playerCount){
        tribeDeck.draw(); // in loop
    }
    private void populateTopRow(TribeDeck tribeDeck, BuildingDeck buildingDeckI, int playerCount){
        tribeDeck.draw(); // in loop
        buildingDeckI.drawAll();
    }

    public CharacterCard findCardById(int cardId) {
        CharacterCard card = topRow.findCardById(cardId);
        if (card != null) return card;
        return bottomRow.findCardById(cardId);
    }

    public void removeCard(int cardId) {
        if (topRow.containsCard(cardId)) topRow.removeCard(cardId);
        else bottomRow.removeCard(cardId);
    }

    public void endRound(TribeDeck tribeDeck, int playerCount) {
        bottomRow.discardTribeCards();
        bottomRow.receiveTribeCards(topRow.extractTribeCardsForBottomRow());
        topRow.restore(tribeDeck, playerCount);
    }

    public BottomRow getBottomRow() {
        return bottomRow;
    }

    public TopRow getTopRow() {
        return topRow;
    }
}