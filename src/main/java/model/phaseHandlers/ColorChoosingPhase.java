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

    public ColorChoosingPhase(GameModel model) {
        this.model = model;
    }

    /**
     * Initializes the state of the phase by populating the available color pool
     * and launching an asynchronous watchdog timer.
     * <p>
     * <b>Timeout Behavior:</b> If the 60-second timer expires before all players
     * have chosen, a background thread forces random assignments for the remaining
     * players and subsequently updates the model.
     * </p>
     */
    @Override
    public void onEnter() {
        availableColors = EnumSet.allOf(TotemColor.class);

        timeout = CompletableFuture.runAsync(() -> {
            List<Player> players = model.getPlayers().stream().filter(p -> p.getColor() == null).toList();
            for(Player player : players) {
                    TotemColor randomizedChoice = availableColors.iterator().next();
                    doChooseColor(player, randomizedChoice);
                }
            model.notifyChange();
        }, CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS));

        model.notifyChange();
    }


    /**
     * Intercepts a color selection request from a client, validating the timing
     * and the payload before delegating to the internal mutation logic.
     * @param cmd the command containing the requesting player's name and desired color.
     * @throws IllegalStateException if the 60-second selection window has already closed.
     * @throws IllegalArgumentException if the provided color string does not map to a valid {@link TotemColor}.
     * @throws Exception if any other processing error occurs.
     */
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


    /**
     * Executes the core transactional logic of assigning a color to a player.
     * <p>
     * This method removes the selected color from the available pool and notifies
     * observers of the state mutation. If this assignment fulfills the requirement
     * that all players have a color, it halts the timeout execution and transitions
     * the game into the {@link SetupPhase}.
     * </p>
     * @param player the player receiving the color assignment.
     * @param color the specific {@link TotemColor} being assigned.
     * @throws IllegalArgumentException if the requested color is already taken by another player.
     */
    private void doChooseColor(Player player, TotemColor color) {
            if (!availableColors.contains(color)) {
                throw new IllegalArgumentException("Color " + color + " is already taken");
            }

            player.setColor(color);
            availableColors.remove(color);

            model.notifyChange();

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
