package model.phaseHandlers;

import model.GameModel;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.player.Player;

import java.util.EnumSet;
import java.util.Set;

public class ColorChoosingPhase extends GamePhaseHandler {

    private Set<TotemColor> availableColors;
    private int currentIndex;
    private Player currentPlayer;

    public ColorChoosingPhase(GameModel model) {
        super(model);
    }

    @Override
    public void onEnter() {
        availableColors = EnumSet.allOf(TotemColor.class);
        currentIndex = 0;
        currentPlayer = model.getPlayers().get(currentIndex);

        model.notifyChange("color_choosing_started:" + currentPlayer.getName());
    }

    @Override
    public void chooseColor(Player player,  TotemColor color) {
        if (player != currentPlayer) {
            //will never get here because the gameController.resolveCurrentPlayer already checks this, better to maintain double check for safety
            throw new IllegalArgumentException("It's not " + player.getName() + "'s turn to choose a color");
        }
        if(!availableColors.contains(color)) {
            //only throws color is already taken because in gameController.chooseColor() totemColor.valueOf(colorName.toUpperCase()) we check that the color is one of the enum
            throw new IllegalArgumentException("Color " + color + " is already taken");
        }


        player.setColor(color);
        availableColors.remove(color);

        advanceTurn();
    }

    private void advanceTurn() {
        currentIndex++;

        if (currentIndex < model.getPlayerCount()) {
            currentPlayer = model.getPlayers().get(currentIndex);
            model.notifyChange("color_choosing_next:" + currentPlayer.getName());
        } else {
            // all players have chosen → proceed to setup
            model.notifyChange("color_choosing_completed");
            model.setPhase(new SetupPhase(model));
        }
    }

    @Override
    public GamePhase getPhase() { return GamePhase.COLOR_CHOOSING_PHASE; }

    @Override
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
}
