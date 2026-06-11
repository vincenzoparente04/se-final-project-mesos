package model.phaseHandlers;

import model.enums.GamePhase;
import model.player.Player;
import shared.command.gameCommand.ChooseColorCommand;
import shared.command.gameCommand.DrawCardCommand;
import shared.command.gameCommand.EndTurnCommand;
import shared.command.gameCommand.GameCommandVisitor;
import shared.command.gameCommand.PlaceTotemCommand;

/**
 * Defines the behavior of a game phase handler.
 * 
 * This interface manages the logic and command processing for a specific game phase.
 * It implements the visitor pattern to handle different types of game commands and ensures
 * that only current phase related commands are processed during each phase.
 * 
 * @see GamePhase
 * @see GameCommandVisitor
 */
public interface GamePhaseHandler extends GameCommandVisitor {

    /**
     * Returns the game phase associated with this handler.
     *
     * @return the current {@link GamePhase}
     */
    GamePhase getPhase();

    /**
     * Returns the player whose turn it is in this phase.
     *
     * @return the current {@link Player}
     */
    Player getCurrentPlayer();

    /**
     * Called when this phase begins.
     * 
     * This method can be overridden by implementations to perform initialization
     * or setup tasks when the phase is activated.
     */
    default void onEnter() {}

    /**
     * Skips the turn of the current player and advances to the next phase or player.
     * 
     * This method can be overridden by implementations to handle turn skipping logic.
     */
    default void skipCurrentPlayerTurn() {}

    /**
     * Processes a color choice command.
     * 
     * This default implementation throws an exception as color selection is not
     * allowed in the current phase.
     *
     * @param cmd the color choice command to process
     * @throws IllegalStateException if color selection is not valid in this phase
     */
    @Override
    default void visit(ChooseColorCommand cmd) throws Exception {
        throw new IllegalStateException("Cannot choose color in phase " + getPhase());
    }

    /**
     * Processes a totem placement command.
     * 
     * This default implementation throws an exception as totem placement is not
     * allowed in the current phase.
     *
     * @param cmd the totem placement command to process
     * @throws IllegalStateException if totem placement is not valid in this phase
     */
    @Override
    default void visit(PlaceTotemCommand cmd) throws Exception {
        throw new IllegalStateException("Cannot place totem in phase " + getPhase());
    }

    /**
     * Processes a card draw command.
     * 
     * This default implementation throws an exception as card drawing is not
     * allowed in the current phase.
     *
     * @param cmd the card draw command to process
     * @throws IllegalStateException if card drawing is not valid in this phase
     */
    @Override
    default void visit(DrawCardCommand cmd) throws Exception {
        throw new IllegalStateException("Cannot draw card in phase " + getPhase());
    }

    /**
     * Processes an end turn command.
     * 
     * This default implementation throws an exception as ending turn is not
     * allowed in the current phase.
     *
     * @param cmd the end turn command to process
     * @throws IllegalStateException if ending turn is not valid in this phase
     */
    @Override
    default void visit(EndTurnCommand cmd) throws Exception {
        throw new IllegalStateException("Cannot end turn in phase " + getPhase());
    }
}
