package model.cards.characterCards;

import model.rowsManager.CardVisitor;
import model.cards.TribeCard;
import model.enums.Era;

/**
 * Abstract base for all character cards. Concrete subclasses represent the
 * different tribe member types (Artist, Builder, Gatherer, Hunter, Inventor,
 * Shaman) and implement {@link model.cards.TribeCard#registerToTribe(model.player.Player)}
 * to enrol themselves in the player's tribe upon being drawn.
 */
public abstract class CharacterCard extends TribeCard {
    public CharacterCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}