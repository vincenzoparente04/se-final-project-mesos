package model.board.OfferTileAction;

/**
 * Visitor for {@link OfferTileAction} subtypes.
 * Implement this interface in the server layer to inspect tile actions
 * without instanceof or type casts in the model.
 */
public interface OfferTileActionVisitor {
    void visitDrawCards(DrawCardsAction action);
    void visitTakeFood(TakeFoodAction action);
}
