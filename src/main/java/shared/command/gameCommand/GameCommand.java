package shared.command.gameCommand;

import shared.command.ClientCommand;
import shared.command.ClientCommandVisitor;

/**
 * Family of commands that mutate the state of an ongoing game (choose colour,
 * place totem, draw card, end turn). It adds a second dispatch level towards
 * {@link GameCommandVisitor}, while the top-level dispatch always routes to
 * {@link ClientCommandVisitor#visit(GameCommand)}.
 */
public interface GameCommand extends ClientCommand {

    /**
     * Double dispatch towards the second-level game-command visitor.
     *
     * @param visitor visitor handling the concrete subtype
     * @throws Exception if handling the command fails
     */
    void accept(GameCommandVisitor visitor) throws Exception;

    @Override
    default void accept(ClientCommandVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
