package model.cards.charachterCards;

import model.GameModel;
import model.board.CardVisitor;
import model.buildingEffects.OnCharacterAcquiredEffects.OnAcquireBuildingEffect;
import model.cards.Drawable;
import model.cards.TribeCard;
import model.enums.Era;
import model.player.Player;

public abstract class CharacterCard extends TribeCard {
    public CharacterCard(int id, Era era, int playerCount) {
        super(id, era, playerCount);
    }

    @Override
    public boolean canBeAcquiredBy(Player player, GameModel model){
        return true;
    }

    @Override
    public void acquiredBy(Player player, GameModel model){
        registerToTribe(player);

        // checks for OnAcquire effects
        for (OnAcquireBuildingEffect effect : player.getTribe().getOnAcquireBuildingEffects()) {
            effect.applyEffect(player);
        }
    }

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }
}