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

        model.notifyChange();
    }

    @Override
    public void chooseColor(Player player,  TotemColor color) {
        if (player != currentPlayer || !availableColors.contains(color)) {return;}


        player.setColor(color);
        availableColors.remove(color);

        advanceTurn();
    }

    private void advanceTurn() {
        currentIndex++;

        if (currentIndex < model.getPlayerCount()) {
            currentPlayer = model.getPlayers().get(currentIndex);
            model.notifyChange();
        } else {
            // all players have chosen → proceed to setup
            model.notifyChange();
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
