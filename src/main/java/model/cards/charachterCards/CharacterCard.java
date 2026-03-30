package model.cards.charachterCards;

import model.GameModel;
import model.rowsManager.CardVisitor;
import model.buildingEffects.OnCharacterAcquiredEffects.OnAcquireBuildingEffect;
import model.cards.TribeCard;
import model.enums.Era;
import model.player.Player;

public abstract class CharacterCard extends TribeCard {
    public CharacterCard(int id, Era era, int playerCount, String imagePath, String backImagePath) {
        super(id, era, playerCount, imagePath, backImagePath);
    }

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}