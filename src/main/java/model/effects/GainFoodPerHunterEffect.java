package model.effects;

import model.GameModel;
import model.enums.CharacterType;
import model.player.Player;

public class GainFoodPerHunterEffect implements ImmediateEffect {

    @Override
    public void apply(Player player, GameModel model) {
        int hunters = player.getTribe().countByType(CharacterType.HUNTER);
        player.addFood(hunters);
    }
}

