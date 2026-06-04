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

/**
 * Handles the preliminary phase of the game where players select their
 * representative totem colors before the match officially begins.
 *
 * <p>This handler is responsible for the following lifecycle:
 * <ol>
 * <li>Initializing the pool of available {@link TotemColor}s.</li>
 * <li>Starting an asynchronous 60-second countdown timer.</li>
 * <li>Processing incoming {@link ChooseColorCommand}s via double-dispatch
 * to validate and assign colors dynamically.</li>
 * <li>Automatically advancing the state machine to the {@link SetupPhase}
 * once all players have successfully locked in a color.</li>
 * <li>Applying a fallback mechanism if the timeout expires, automatically
 * assigning random remaining colors to any inactive players to prevent server deadlocks.</li>
 * </ol>
 *
 * <p>Command dispatch utilizes the <em>Visitor</em> pattern to intercept client requests,
 * ensuring robust validation against duplicate choices or late submissions.
 *
 * @see GamePhaseHandler
 * @see shared.command.gameCommand.ChooseColorCommand
 * @see model.phaseHandlers.SetupPhase
 */
public class ColorChoosingPhase implements GamePhaseHandler {

    private final GameModel model;
    private Set<TotemColor> availableColors;
    private CompletableFuture<Void> timeout;
    private Player currentPlayer;

    public ColorChoosingPhase(GameModel model) {
        this.model = model;
    }

    @Override
    public void onEnter() {
        availableColors = EnumSet.allOf(TotemColor.class);

        timeout = CompletableFuture.runAsync(() -> {
            List<Player> players = model.getPlayers().stream().filter(p -> p.getColor() == null).toList();
            for (Player player : players) {
                TotemColor randomizedChoice = availableColors.iterator().next();
                doChooseColor(player, randomizedChoice);
            }
        }, CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS));

        List<Player> players = model.getPlayers();
        currentPlayer = players.isEmpty() ? null : players.get(0);

        model.notifyChange();
    }

    @Override
    public void visit(ChooseColorCommand cmd) throws Exception {
        if (timeout.isDone()) {
            throw new IllegalStateException("Time to choose a color has already ended");
        }

        Player player = model.getPlayerByName(cmd.playerName());

        if (!player.equals(currentPlayer)) {
            throw new IllegalArgumentException("It is not " + cmd.playerName() + "'s turn to choose a color");
        }

        TotemColor color;
        try {
            color = TotemColor.valueOf(cmd.color().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid color: " + cmd.color());
        }
        doChooseColor(player, color);
    }

    @Override
    public void skipCurrentPlayerTurn() {
        if (currentPlayer == null) return;
        Player toSkip = currentPlayer;
        TotemColor randomColor = availableColors.iterator().next();
        doChooseColor(toSkip, randomColor);
    }

    private void doChooseColor(Player player, TotemColor color) {
        if (!availableColors.contains(color)) {
            throw new IllegalArgumentException("Color " + color + " is already taken");
        }

        player.setColor(color);
        availableColors.remove(color);

        model.notifyChange();

        advanceCurrentPlayer(player);
    }

    private void advanceCurrentPlayer(Player justChose) {
        List<Player> players = model.getPlayers();
        int startIdx = players.indexOf(justChose) + 1;

        for (int i = startIdx; i < players.size(); i++) {
            Player next = players.get(i);
            if (next.getColor() == null) {
                if (next.isConnected()) {
                    currentPlayer = next;
                    return;
                } else {
                    // Auto-assign a color to disconnected player and keep scanning
                    TotemColor randomColor = availableColors.iterator().next();
                    next.setColor(randomColor);
                    availableColors.remove(randomColor);
                }
            }
        }

        // No more players need to choose
        currentPlayer = null;
        timeout.complete(null);
        model.setPhase(new SetupPhase(model));
    }

    @Override
    public GamePhase getPhase() { return GamePhase.COLOR_CHOOSING_PHASE; }

    @Override
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
}
