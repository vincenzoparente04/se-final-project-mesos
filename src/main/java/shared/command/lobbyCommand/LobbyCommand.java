package shared.command.lobbyCommand;

import shared.command.ClientCommand;
import shared.command.ClientCommandVisitor;

public interface LobbyCommand extends ClientCommand {

    void accept(LobbyCommandVisitor visitor) throws Exception;

    @Override
    default void accept(ClientCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
