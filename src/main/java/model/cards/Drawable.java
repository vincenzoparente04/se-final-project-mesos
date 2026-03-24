package model.cards;

import model.GameModel;
import model.player.Player;

public interface Drawable {
    /**
     * @implSpec This method is called to check if a player can acquire this card.
     * @param player
     * @param model
     * @return
     */
    boolean canBeAcquiredBy(Player player, GameModel model);

    /**
     * @implSpec This method is called when a player acquires this card, after checking that the player can acquire it.
     * @param player
     * @param model
     */
    void acquiredBy(Player player, GameModel model);
}
