package shared.command;

/**
 * Visitor interface for the {@link GameCommand} hierarchy.
 * <p>
 * Each command type has a corresponding {@code visit} overload.
 * Implementors (e.g. {@code ControllerCommandExecutor}) provide the
 * actual execution logic without relying on {@code instanceof} or
 * switch statements.
 */
public interface CommandVisitor {
    void visit(ChooseColorCommand command) throws Exception;
    void visit(PlaceTotemCommand command) throws Exception;
    void visit(DrawCardCommand command) throws Exception;
    void visit(EndTurnCommand command) throws Exception;
}
