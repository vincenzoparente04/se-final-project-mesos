package shared.command;

public sealed interface LobbyCommand extends ClientCommand
        permits CreateLobbyCommand, JoinLobbyCommand, ListLobbiesCommand {

    @Override
    default void accept(CommandDispatcher dispatcher) throws Exception {
        dispatcher.onLobbyCommand(this);
    }

    void accept(LobbyCommandVisitor visitor) throws Exception;
}
