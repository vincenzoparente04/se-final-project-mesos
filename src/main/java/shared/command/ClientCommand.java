package shared.command;

import java.io.Serializable;

public sealed interface ClientCommand extends Serializable
        permits GameCommand, HeartbeatCommand, LobbyCommand {

    void accept(CommandDispatcher dispatcher) throws Exception;

    String getPlayerName();
}
