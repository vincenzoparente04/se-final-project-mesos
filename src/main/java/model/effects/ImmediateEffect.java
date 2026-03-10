package model.effects;

import model.GameModel;
import model.player.Player;

// Interface for the immediate effects of the card, it is the bridge between the card and the Controller, which is responsible for applying the effect to the game state.
// The card only describes the effect, while the Controller is responsible for applying it to the game state.
public interface ImmediateEffect {

    void apply(Player player, GameModel model);

    String getDescription(); // ### bo non ho ben capito a che serve ###
}
