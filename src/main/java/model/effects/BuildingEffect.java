package model.effects;


import model.player.Player;

public interface BuildingEffect {
    BuildingEffectTrigger getTrigger();
    // DURING_HUNT, DURING_SHAMANIC_RITUAL, END_OF_GAME...

    void apply(Player player, GameModel model);
    String getDescription(); // ### anche qua non ho capito che fa ###
}