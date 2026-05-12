package model.phaseHandlers;

import model.GameModel;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.player.Player;
import shared.command.ChooseColorCommand;

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

        if (!currentPlayer.isConnected()) {
            advanceTurn();
            return;
        }

        model.notifyChange();
    }

    @Override
    public void visit(ChooseColorCommand cmd) throws Exception {
        Player player = model.getPlayerByName(cmd.playerName());
        TotemColor color;
        try {
            color = TotemColor.valueOf(cmd.color().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid color: " + cmd.color());
        }
        doChooseColor(player, color);
    }

    // TODO: forse si può tenere un metodo unico nel visit che fa tutto e skippiamo il doppio controllo sul player
    private void doChooseColor(Player player, TotemColor color) {
        if (player != currentPlayer) {
            //will never get here because the gameController.handleCommand already checks this, better to maintain double check for safety
            throw new IllegalArgumentException("It's not " + player.getName() + "'s turn to choose a color");
        }
        if (!availableColors.contains(color)) {
            //only throws color is already taken because in visit() we check that the color is one of the enum
            throw new IllegalArgumentException("Color " + color + " is already taken");
        }

        player.setColor(color);
        availableColors.remove(color);

        advanceTurn();
    }

    private void advanceTurn() {
        // get the next player; if it's a disconnected one it skips him and assign random color
        do {
            currentIndex++;

            if (currentIndex >= model.getPlayerCount()) {
                // all players have chosen → proceed to setup
                model.notifyChange();
                model.setPhase(new SetupPhase(model));
                return;
            }

            currentPlayer = model.getPlayers().get(currentIndex);

            if (!currentPlayer.isConnected()) {
                // Player is disconnected → assign random color and continue loop
                TotemColor randomizedChoice = availableColors.iterator().next();
                currentPlayer.setColor(randomizedChoice);
                availableColors.remove(randomizedChoice);
                // Loop continues to next player
            }
        } while (!currentPlayer.isConnected());  // Exit loop when we find a connected player

        model.notifyChange();
    }

    @Override
    /**
     * @implNote if a player disconnects during the color choosing phase,
     * we will assign him a random available color and move on to the next player.
     * This is to ensure that the game can proceed even if a player disconnects at the very beginning of the game.
     * The random assignment is done by simply taking the next available color from the set of available colors,
     * which is sufficient for our purposes since the order of color assignment does not matter in this phase.
     */
    public void skipCurrentPlayerTurn() {
        currentPlayer = model.getPlayers().get(currentIndex);
        TotemColor randomizedChoice = availableColors.iterator().next();
        doChooseColor(currentPlayer, randomizedChoice);
        model.notifyChange();
    }

    @Override
    public GamePhase getPhase() { return GamePhase.COLOR_CHOOSING_PHASE; }

    @Override
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
}
