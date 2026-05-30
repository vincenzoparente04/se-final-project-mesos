package model.phaseHandlers;

import model.enums.GamePhase;
import model.player.Player;
import shared.command.gameCommand.ChooseColorCommand;
import shared.command.gameCommand.DrawCardCommand;
import shared.command.gameCommand.EndTurnCommand;
import shared.command.gameCommand.GameCommandVisitor;
import shared.command.gameCommand.PlaceTotemCommand;

public interface GamePhaseHandler extends GameCommandVisitor {

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
