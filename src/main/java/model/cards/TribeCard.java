package model.cards;

import model.board.CardVisitor;
import model.enums.Era;
import model.player.Player;

// it's a group of cards which include character e event cards, wich are the types of cards in the deck
public abstract class TribeCard extends Card {
    public TribeCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }

    public abstract void registerSelf(Player player);

    public abstract void accept(CardVisitor visitor);
}