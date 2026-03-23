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
    public void chooseColor(Player player,  TotemColor color) throws IllegalStateException {
        if (player != currentPlayer) {
            throw new IllegalStateException(
                    "It's not " + player.getName() + "'s turn to choose a color. " +
                            "Waiting for " + currentPlayer.getName()
            );
        }

        // Validate: is the color available?
        if (!availableColors.contains(color)) {
            throw new IllegalStateException(
                    "Color " + color + " is not available. Available colors: " + availableColors
            );
        }


        player.setColor(color);
        availableColors.remove(color);
        notifyChange("color_chosen:" + player.getName() + ":" + color);

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
