package shared.command;

public sealed interface GameCommand extends ClientCommand
        permits ChooseColorCommand, DrawCardCommand, EndTurnCommand, PlaceTotemCommand {

    void accept(CommandVisitor visitor) throws Exception;

    @Override
    default void accept(CommandDispatcher dispatcher) throws Exception {
        dispatcher.onGameCommand(this);
    }
}
