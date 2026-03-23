package model.phaseHandlers;

import model.GameModel;
import model.board.OfferTile;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.player.Player;

public abstract class GamePhaseHandler {

    protected final GameModel model;

    public GamePhaseHandler(GameModel model) {
        this.model = model;
    }

    public void onEnter() {}

    public void chooseColor(Player player, TotemColor color) {}

    public void placeTotem(Player player, OfferTile tile) {}

    public void drawCard(int cardId) {
        throw new IllegalStateException(
                "Cannot draw card during " + getPhase()
        );
    }

    public abstract GamePhase getPhase();
    public abstract Player getCurrentPlayer();
}