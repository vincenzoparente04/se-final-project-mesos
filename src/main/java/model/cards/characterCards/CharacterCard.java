package model.cards.characterCards;

import model.rowsManager.CardVisitor;
import model.cards.TribeCard;
import model.enums.Era;

public abstract class CharacterCard extends TribeCard {
    public CharacterCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}