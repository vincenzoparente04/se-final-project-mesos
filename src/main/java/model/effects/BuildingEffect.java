package model.effects;

import model.GameModel;
import model.player.Player;

public interface BuildingEffect {

    public void registerSelf(Player player);
}