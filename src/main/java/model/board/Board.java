package model.board;

// The main board class that contains all the components of the board and provides access to them for the Controller and the View.
// It is only responsible for holding the state of the board and providing access to its components, while the Controller is
// responsible for applying the game logic and mutating the state of the board accordingly; the View is responsible for
// displaying the current state of the board.

public class Board {
    private final OfferTrack offerTrack;
    private final TurnOrderTile turnOrderTile;
    private final TopRow topRow;
    private final BottomRow bottomRow;

    public OfferTrack getOfferTrack(){
        return offerTrack;
    }
    public TurnOrderTile getTurnOrderTile(){
        return turnOrderTile;
    }
    public TopRow getTopRow(){
        return topRow;
    }
    public BottomRow getBottomRow(){
        return bottomRow;
    }
}