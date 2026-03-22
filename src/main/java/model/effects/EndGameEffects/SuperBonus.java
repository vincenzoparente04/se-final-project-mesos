package model.effects.EndGameEffects;

import model.player.Player;

public class SuperBonus extends EndGameEffect {
    private final int superBonus;

    public SuperBonus(int superBonus){
        this.superBonus = superBonus;
    }

    /**
     * @implNote Adds the bonus points, parameter passed the building card factory
     * @param player
     */
    @Override
    public void applyEffect(Player player) {
        player.addPrestigePoints(superBonus);
    }
}
