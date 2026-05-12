package model.phaseHandlers;

import model.enums.GamePhase;
import model.player.Player;
import shared.command.ChooseColorCommand;
import shared.command.CommandVisitor;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.PlaceTotemCommand;

public interface GamePhaseHandler extends CommandVisitor {

    GamePhase getPhase();
    Player getCurrentPlayer();

    default void onEnter() {}
    default void skipCurrentPlayerTurn() {}

    @Override
    default void visit(ChooseColorCommand cmd) throws Exception {
        throw new IllegalStateException("Cannot choose color in phase " + getPhase());
    }

    @Override
    default void visit(PlaceTotemCommand cmd) throws Exception {
        throw new IllegalStateException("Cannot place totem in phase " + getPhase());
    }

    @Override
    default void visit(DrawCardCommand cmd) throws Exception {
        throw new IllegalStateException("Cannot draw card in phase " + getPhase());
    }

    @Override
    default void visit(EndTurnCommand cmd) throws Exception {
        throw new IllegalStateException("Cannot end turn in phase " + getPhase());
    }
}
