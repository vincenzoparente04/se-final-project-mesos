package model.phaseHandlers;

import model.GameModel;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.player.Player;

public abstract class GamePhaseHandler {

    protected final GameModel model;

    public GamePhaseHandler(GameModel model) {
        this.model = model;
    }

    public void onEnter() {}

    public void chooseColor(Player player, TotemColor color) {
        throw new IllegalStateException("Cannot choose color during " + getPhase());
    }

    public void placeTotem(Player player, char tileId) {
        throw new IllegalStateException("Cannot place totem during " + getPhase());
    }

    public void drawCard(int cardId) {
        throw new IllegalStateException("Cannot draw card during " + getPhase());
    }

    public void endTurn() {
        throw new IllegalStateException("Cannot end turn during " + getPhase());
    }

    public abstract GamePhase getPhase();
    public abstract Player getCurrentPlayer();
}