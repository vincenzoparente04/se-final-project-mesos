package model.phaseHandlers;

import model.GameModel;
import model.board.OfferTile;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.player.Player;
import model.player.Totem;

import java.util.EnumSet;
import java.util.Set;

public class ColorChoosingPhase implements GamePhaseHandler {
    private final GameModel model;

    private Set<TotemColor> availableColors;
    private int currentIndex;
    private Player currentPlayer;

    public ColorChoosingPhase(GameModel model) {
        this.model = model;
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

        // Assign the totem to the player with the chosen color
        player.setTotem(new Totem(player, color));

        // Remove the color from available pool
        availableColors.remove(color);

        // Notify observers about the color assignment
        notifyChange("color_chosen:" + player.getName() + ":" + color);

        // ── advance ──
        advanceTurn();
    }

    private void advanceTurn() {
        currentIndex++;

        if (currentIndex < model.getPlayers().size()) {
            currentPlayer = model.getPlayers().get(currentIndex);
            model.notifyChange("color_choosing_next:" + currentPlayer.getName());
        } else {
            // all players have chosen → proceed to setup
            model.notifyChange("color_choosing_completed");
            model.setPhase(new SetupPhase(model));
        }
    }

    @Override
    public void placeTotem(Player player, OfferTile offerTile) {

    }

    @Override
    public void drawCard(int cardId) {

    }

    @Override
    public GamePhase getPhase() {
        return null;
    }

    @Override
    public Player getCurrentPlayer() {
        return null;
    }


}
