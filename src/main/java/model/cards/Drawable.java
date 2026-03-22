package model.cards;

import model.GameModel;
import model.player.Player;

public interface Drawable {
    boolean canBeAcquiredBy(Player player, GameModel model);
    void acquiredBy(Player player, GameModel model);
}
