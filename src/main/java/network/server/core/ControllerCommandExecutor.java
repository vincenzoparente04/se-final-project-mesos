package network.server.core;

import controller.GameController;
import shared.command.ChooseColorCommand;
import shared.command.CommandVisitor;
import shared.command.DrawCardCommand;
import shared.command.EndTurnCommand;
import shared.command.PlaceTotemCommand;

/**
 * {@link CommandVisitor} implementation that delegates each command to the
 * corresponding {@link GameController} method.
 * <p>
 * Used by {@link QueueDrainerThread} to execute commands dequeued from the central
 * {@code BlockingQueue}.  Because the visitor dispatch is polymorphic, no
 * {@code instanceof} or {@code switch} is needed here.
 */
public class ControllerCommandExecutor implements CommandVisitor {

    private final GameController controller;

    public ControllerCommandExecutor(GameController controller) {
        this.controller = controller;
    }

    @Override
    public void visit(ChooseColorCommand command) throws Exception {
        controller.chooseColor(command.playerName(), command.color());
    }

    @Override
    public void visit(PlaceTotemCommand command) throws Exception {
        controller.placeTotem(command.playerName(), command.tileId());
    }

    @Override
    public void visit(DrawCardCommand command) throws Exception {
        controller.drawCard(command.playerName(), command.cardId());
    }

    @Override
    public void visit(EndTurnCommand command) throws Exception {
        controller.endTurn(command.playerName());
    }
}
