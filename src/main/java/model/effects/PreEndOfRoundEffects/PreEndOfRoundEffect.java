package model.effects.PreEndOfRoundEffects;

import model.board.Board;
import model.effects.BuildingEffect;
import model.player.Player;

public abstract class PreEndOfRoundEffect implements BuildingEffect {

    @Override
    public void registerSelf(Player player) {

    }

    public abstract void applyEffect(Player player, Board board);
}
