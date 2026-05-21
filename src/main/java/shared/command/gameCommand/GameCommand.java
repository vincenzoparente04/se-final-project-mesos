package shared.command.gameCommand;

import shared.command.ClientCommand;
import shared.command.ClientCommandVisitor;

public interface GameCommand extends ClientCommand {

    void accept(GameCommandVisitor visitor) throws Exception;

    @Override
    default void accept(ClientCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
