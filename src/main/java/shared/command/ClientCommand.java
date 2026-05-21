package shared.command;

import java.io.Serializable;

public interface ClientCommand extends Serializable {

    void accept(ClientCommandVisitor visitor) throws Exception;

    String getPlayerName();
}
