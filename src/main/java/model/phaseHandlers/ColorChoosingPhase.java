package model.phaseHandlers;

import model.GameModel;
import model.enums.GamePhase;
import model.enums.TotemColor;
import model.player.Player;
import shared.command.gameCommand.ChooseColorCommand;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ColorChoosingPhase implements GamePhaseHandler {

    private final GameModel model;
    private Set<TotemColor> availableColors;
    private CompletableFuture<Void> timeout;

    public ColorChoosingPhase(GameModel model) {
        this.model = model;
    }

    @Override
    public void onEnter() {
        availableColors = EnumSet.allOf(TotemColor.class);

        // timeout start
        timeout = CompletableFuture.runAsync(() -> {
            List<Player> players = model.getPlayers().stream().filter(p -> p.getColor() == null).toList(); //player color is initialized as null in Player class
            for(Player player : players) {
                    TotemColor randomizedChoice = availableColors.iterator().next();
                    doChooseColor(player, randomizedChoice);
                }
            model.notifyChange();
        }, CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS));

        model.notifyChange();
    }

    @Override
    public void visit(ChooseColorCommand cmd) throws Exception {
        if (timeout.isDone()) {
            throw new IllegalStateException("Time to choose a color has already ended");
        }
        Player player = model.getPlayerByName(cmd.playerName());
        TotemColor color;
        try {
            color = TotemColor.valueOf(cmd.color().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid color: " + cmd.color());
        }
        doChooseColor(player, color);
    }

    private void doChooseColor(Player player, TotemColor color) {
            if (!availableColors.contains(color)) {
                throw new IllegalArgumentException("Color " + color + " is already taken");
            }

            player.setColor(color);
            availableColors.remove(color);

            model.notifyChange();
            // check if every player has chosen a color
            if (availableColors.size() == TotemColor.values().length - model.getPlayerCount()) {
                timeout.complete(null); // stop the timeout if it's still running
                model.setPhase(new SetupPhase(model));
            }
    }

    @Override
    public GamePhase getPhase() { return GamePhase.COLOR_CHOOSING_PHASE; }

    @Override
    public Player getCurrentPlayer() {
        return null;
    }
}
